#!/bin/bash
#===============================================================================
# 全自动 MySQL InnoDB 数据恢复脚本
# 功能: 从 .ibd 文件自动解析表结构并恢复数据
# 用法: ./auto_restore_ibd.sh /旧数据目录 [数据库名]
# 示例: ./auto_restore_ibd.sh /var/lib/mysql_old/eum_db eum_db
#===============================================================================

OLD_DB_DIR="$1"
DB_NAME="${2:-$(basename "$OLD_DB_DIR")}"
MYSQL_DATA="/var/lib/mysql"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[OK]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }
print_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }

# 创建 Python 解析脚本
create_python_parser() {
    cat > /tmp/parse_ibd.py << 'PYEOF'
#!/usr/bin/env python3
import sys
import json

def parse_ibd(json_str):
    try:
        data = json.loads(json_str)
    except Exception as e:
        return None
    
    for obj in data:
        if isinstance(obj, str):
            continue
        if not isinstance(obj, dict):
            continue
        if obj.get('type') != 1:
            continue
        
        dd = obj.get('object', {}).get('dd_object', {})
        if not dd:
            continue
        
        table_name = dd.get('name', '')
        columns = dd.get('columns', [])
        indexes = dd.get('indexes', [])
        
        if not table_name or not columns:
            continue
        
        col_defs = []
        pk_cols = []
        unique_keys = []
        normal_keys = []
        
        for idx in indexes:
            idx_name = idx.get('name', '')
            idx_type = idx.get('type', 0)
            elements = idx.get('elements', [])
            col_positions = [e.get('column_opx', -1) for e in elements]
            
            if idx_name == 'PRIMARY':
                pk_cols = col_positions
            elif idx_type == 2:
                unique_keys.append((idx_name, col_positions))
            elif idx_type == 3:
                normal_keys.append((idx_name, col_positions))
        
        visible_cols = []
        for col in columns:
            name = col.get('name', '')
            if not name or name.startswith('DB_'):
                continue
            
            col_type = col.get('column_type_utf8', '')
            if not col_type:
                continue
            
            nullable = col.get('is_nullable', True)
            null_str = '' if nullable else ' NOT NULL'
            
            auto_inc = col.get('is_auto_increment', False)
            auto_str = ' AUTO_INCREMENT' if auto_inc else ''
            
            default_str = ''
            default_val = col.get('default_value_utf8')
            default_null = col.get('default_value_utf8_null', True)
            has_no_default = col.get('has_no_default', True)
            
            if not has_no_default and not auto_inc:
                if default_null or default_val == '':
                    if nullable:
                        default_str = ' DEFAULT NULL'
                elif default_val is not None:
                    if col_type.lower().startswith(('int', 'bigint', 'smallint', 'tinyint', 'decimal', 'float', 'double', 'mediumint')):
                        default_str = f' DEFAULT {default_val}'
                    else:
                        # 转义单引号
                        safe_val = str(default_val).replace("'", "\\'")
                        default_str = f" DEFAULT '{safe_val}'"
            
            comment = col.get('comment', '')
            # 转义单引号
            if comment:
                comment = comment.replace("'", "\\'")
            comment_str = f" COMMENT '{comment}'" if comment else ''
            
            col_defs.append(f'`{name}` {col_type}{null_str}{default_str}{auto_str}{comment_str}')
            visible_cols.append(name)
        
        if not col_defs:
            continue
        
        if pk_cols:
            pk_names = [f'`{visible_cols[i]}`' for i in pk_cols if i < len(visible_cols)]
            if pk_names:
                col_defs.append(f'PRIMARY KEY ({", ".join(pk_names)})')
        
        for idx_name, positions in unique_keys:
            idx_cols = [f'`{visible_cols[i]}`' for i in positions if i < len(visible_cols)]
            if idx_cols:
                col_defs.append(f'UNIQUE KEY `{idx_name}` ({", ".join(idx_cols)})')
        
        for idx_name, positions in normal_keys:
            idx_cols = [f'`{visible_cols[i]}`' for i in positions if i < len(visible_cols)]
            if idx_cols:
                col_defs.append(f'KEY `{idx_name}` ({", ".join(idx_cols)})')
        
        sql = f'CREATE TABLE IF NOT EXISTS `{table_name}` (\n  '
        sql += ',\n  '.join(col_defs)
        sql += '\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;'
        
        return sql
    
    return None

if __name__ == '__main__':
    json_str = sys.stdin.read()
    result = parse_ibd(json_str)
    if result:
        print(result)
        sys.exit(0)
    else:
        sys.exit(1)
PYEOF
    chmod +x /tmp/parse_ibd.py
}

# 从 .ibd 文件生成 CREATE TABLE 语句
generate_ddl() {
    local ibd_file="$1"
    ibd2sdi "$ibd_file" 2>/dev/null | python3 /tmp/parse_ibd.py 2>/dev/null
}

# 恢复单个表
restore_table() {
    local ibd_file="$1"
    local table_name=$(basename "$ibd_file" .ibd)
    
    echo ""
    print_info "处理表: $table_name"
    
    # 生成 DDL
    local ddl=$(generate_ddl "$ibd_file")
    
    if [ -z "$ddl" ]; then
        print_error "无法解析表结构: $table_name"
        echo "  调试: 检查 ibd2sdi 输出..."
        ibd2sdi "$ibd_file" 2>&1 | head -30
        return 1
    fi
    
    # 显示生成的 DDL（调试）
    echo "  生成的DDL:"
    echo "$ddl" | head -5
    echo "  ..."
    
    # 先删除可能存在的表
    mysql -u root -e "DROP TABLE IF EXISTS \`${DB_NAME}\`.\`${table_name}\`;" 2>/dev/null
    
    # 创建表
    local create_result=$(echo "$ddl" | mysql -u root "$DB_NAME" 2>&1)
    if [ $? -ne 0 ]; then
        print_error "创建表失败: $table_name"
        echo "  错误信息: $create_result"
        echo "  完整DDL:"
        echo "$ddl"
        return 1
    fi
    print_success "表结构已创建"
    
    # 丢弃表空间
    local discard_result=$(mysql -u root -e "ALTER TABLE \`${DB_NAME}\`.\`${table_name}\` DISCARD TABLESPACE;" 2>&1)
    if [ $? -ne 0 ]; then
        print_error "DISCARD TABLESPACE 失败"
        echo "  错误: $discard_result"
        
        # 检查 innodb_file_per_table 设置
        local fpt=$(mysql -u root -N -e "SHOW VARIABLES LIKE 'innodb_file_per_table';" 2>/dev/null | awk '{print $2}')
        echo "  innodb_file_per_table = $fpt"
        
        if [ "$fpt" != "ON" ]; then
            print_warn "需要开启 innodb_file_per_table"
            mysql -u root -e "SET GLOBAL innodb_file_per_table=ON;" 2>/dev/null
            # 删除并重建表
            mysql -u root -e "DROP TABLE \`${DB_NAME}\`.\`${table_name}\`;" 2>/dev/null
            echo "$ddl" | mysql -u root "$DB_NAME" 2>/dev/null
            discard_result=$(mysql -u root -e "ALTER TABLE \`${DB_NAME}\`.\`${table_name}\` DISCARD TABLESPACE;" 2>&1)
            if [ $? -ne 0 ]; then
                print_error "重试后仍然失败: $discard_result"
                return 1
            fi
        else
            return 1
        fi
    fi
    print_success "DISCARD TABLESPACE 成功"
    
    # 检查表是否还存在
    local table_exists=$(mysql -u root -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${DB_NAME}' AND table_name='${table_name}';" 2>/dev/null)
    echo "  表存在检查: $table_exists"
    
    # 检查数据目录
    echo "  数据目录: ${MYSQL_DATA}/${DB_NAME}/"
    ls -la "${MYSQL_DATA}/${DB_NAME}/" 2>/dev/null | grep -E "^-|${table_name}" | head -5
    
    # 复制 .ibd 文件
    echo "  复制文件: $ibd_file -> ${MYSQL_DATA}/${DB_NAME}/"
    cp "$ibd_file" "${MYSQL_DATA}/${DB_NAME}/"
    if [ $? -ne 0 ]; then
        print_error "复制文件失败"
        return 1
    fi
    
    chown mysql:mysql "${MYSQL_DATA}/${DB_NAME}/${table_name}.ibd"
    chmod 640 "${MYSQL_DATA}/${DB_NAME}/${table_name}.ibd"
    
    # 验证文件复制
    echo "  文件验证:"
    ls -la "${MYSQL_DATA}/${DB_NAME}/${table_name}.ibd" 2>/dev/null
    
    # 导入表空间
    echo "  执行 IMPORT TABLESPACE..."
    local result=$(mysql -u root -e "ALTER TABLE \`${DB_NAME}\`.\`${table_name}\` IMPORT TABLESPACE;" 2>&1)
    
    if echo "$result" | grep -qi "error"; then
        print_error "IMPORT TABLESPACE 失败: $result"
        return 1
    fi
    
    local count=$(mysql -u root -N -e "SELECT COUNT(*) FROM \`${DB_NAME}\`.\`${table_name}\`;" 2>/dev/null)
    print_success "数据恢复成功，行数: ${count:-0}"
    
    return 0
}

#===============================================================================
# 主程序
#===============================================================================

if [ -z "$OLD_DB_DIR" ]; then
    echo "用法: $0 <旧数据目录> [数据库名]"
    echo "示例: $0 /var/lib/mysql_old/eum_db eum_db"
    exit 1
fi

if [ ! -d "$OLD_DB_DIR" ]; then
    print_error "目录不存在: $OLD_DB_DIR"
    exit 1
fi

ibd_count=$(ls -1 "$OLD_DB_DIR"/*.ibd 2>/dev/null | wc -l)
if [ "$ibd_count" -eq 0 ]; then
    print_error "目录中没有 .ibd 文件"
    exit 1
fi

echo ""
echo "============================================"
echo "     MySQL InnoDB 数据自动恢复工具"
echo "============================================"
echo ""
echo "数据源目录: $OLD_DB_DIR"
echo "目标数据库: $DB_NAME"
echo "发现表数量: $ibd_count"
echo ""

# 检查命令
for cmd in ibd2sdi python3 mysql; do
    if ! command -v $cmd &>/dev/null; then
        print_error "命令不存在: $cmd"
        exit 1
    fi
done

# 创建 Python 解析脚本
create_python_parser

# 测试解析器
test_result=$(ibd2sdi "$OLD_DB_DIR"/*.ibd 2>/dev/null | head -1000 | python3 /tmp/parse_ibd.py 2>/dev/null)
if [ -z "$test_result" ]; then
    print_warn "解析器测试: 请确保 ibd2sdi 输出正常"
fi

# 输入密码
read -sp "请输入 MySQL root 密码: " MYSQL_PWD
echo ""
export MYSQL_PWD

if ! mysql -u root -e "SELECT 1" &>/dev/null; then
    print_error "MySQL 连接失败"
    exit 1
fi
print_success "MySQL 连接成功"

# 完全重置数据库
print_info "清理旧数据..."
mysql -u root -e "DROP DATABASE IF EXISTS \`${DB_NAME}\`;" 2>/dev/null
rm -rf "${MYSQL_DATA}/${DB_NAME}" 2>/dev/null
sleep 1

mysql -u root -e "CREATE DATABASE \`${DB_NAME}\` DEFAULT CHARSET utf8mb4;"
print_success "数据库 ${DB_NAME} 已创建"

# 确保 innodb_file_per_table 开启
mysql -u root -e "SET GLOBAL innodb_file_per_table=ON;" 2>/dev/null
fpt=$(mysql -u root -N -e "SHOW VARIABLES LIKE 'innodb_file_per_table';" | awk '{print $2}')
print_info "innodb_file_per_table = $fpt"

success_count=0
fail_count=0
total_rows=0

for ibd_file in "$OLD_DB_DIR"/*.ibd; do
    [ -f "$ibd_file" ] || continue
    
    if restore_table "$ibd_file"; then
        ((success_count++))
        table_name=$(basename "$ibd_file" .ibd)
        rows=$(mysql -u root -N -e "SELECT COUNT(*) FROM \`${DB_NAME}\`.\`${table_name}\`;" 2>/dev/null)
        total_rows=$((total_rows + rows))
    else
        ((fail_count++))
    fi
done

unset MYSQL_PWD
rm -f /tmp/parse_ibd.py

echo ""
echo "============================================"
echo "              恢复完成"
echo "============================================"
echo ""
echo -e "成功: ${GREEN}${success_count}${NC} 个表"
echo -e "失败: ${RED}${fail_count}${NC} 个表"
echo -e "总行数: ${BLUE}${total_rows}${NC}"
echo ""

if [ $success_count -gt 0 ]; then
    echo "验证: mysql -u root -p -e 'USE ${DB_NAME}; SHOW TABLES;'"
fi

exit $fail_count
