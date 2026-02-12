import psycopg2
from .config import get_database_config


def get_database_connection():
    """获取数据库连接"""
    config = get_database_config()
    return psycopg2.connect(
        host=config['host'],
        database=config['database'],
        user=config['user'],
        password=config['password'],
        port=config['port']
    )


def get_database_cursor():
    """获取数据库游标"""
    connection = get_database_connection()
    return connection.cursor()