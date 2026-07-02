import os
from dotenv import load_dotenv
from sqlalchemy import create_engine, text

load_dotenv()

# 从环境变量读取配置
DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = os.getenv("DB_PORT", "5432")
DB_USER = os.getenv("DB_USER", "postgres")
DB_PASSWORD = os.getenv("DB_PASSWORD", "")
DB_NAME = os.getenv("DB_NAME", "postgres")

# 构建连接URL
DATABASE_URL = f"postgresql://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}"

print(f"📡 数据库连接URL: postgresql://{DB_USER}:***@{DB_HOST}:{DB_PORT}/{DB_NAME}")

try:
    engine = create_engine(DATABASE_URL)
    with engine.connect() as conn:
        # 测试连接
        result = conn.execute(text("SELECT 1")).fetchone()
        print(f"✅ 数据库连接成功! 测试结果: {result}")

        # 查看所有表
        tables = conn.execute(text("""
                                   SELECT table_name
                                   FROM information_schema.tables
                                   WHERE table_schema = 'public'
                                   """)).fetchall()

        table_names = [t[0] for t in tables]
        print(f"📊 数据库中的表: {table_names}")

        # 检查 medical_order 表
        if 'medical_order' in table_names:
            count = conn.execute(text("SELECT COUNT(*) FROM medical_order")).fetchone()
            print(f"📊 medical_order 表记录数: {count[0]}")
        else:
            print("❌ medical_order 表不存在!")

        # 检查 medical_order_item 表
        if 'medical_order_item' in table_names:
            count = conn.execute(text("SELECT COUNT(*) FROM medical_order_item")).fetchone()
            print(f"📊 medical_order_item 表记录数: {count[0]}")
        else:
            print("❌ medical_order_item 表不存在!")

except Exception as e:
    print(f"❌ 连接失败: {e}")