"""Checks that Sentence Transformers, Qdrant (local mode), Ollama and MySQL all work."""
import sys
import httpx
import pymysql
import os
from dotenv import load_dotenv
load_dotenv(os.path.join(os.path.dirname(__file__), "..", ".env"))
from sentence_transformers import SentenceTransformer
from qdrant_client import QdrantClient
from qdrant_client.models import Distance, VectorParams, PointStruct

OLLAMA = "http://localhost:11434"
MYSQL = dict(
    host=os.getenv("MYSQL_HOST", "localhost"),
    port=int(os.getenv("MYSQL_PORT", "3306")),
    user=os.getenv("MYSQL_USER"),
    password=os.getenv("MYSQL_PASSWORD"),
    database=os.getenv("MYSQL_DATABASE"),
)

def check_embeddings():
    model = SentenceTransformer("all-MiniLM-L6-v2")
    vec = model.encode("DocuMind test sentence")
    assert len(vec) == 384, f"Unexpected dimension: {len(vec)}"
    print(f"[OK] Embeddings work. Dimension = {len(vec)}")
    return model


def check_qdrant(model):
    client = QdrantClient(path="./qdrant_storage_test")
    name = "env_check"
    if client.collection_exists(name):
        client.delete_collection(name)
    client.create_collection(name, vectors_config=VectorParams(size=384, distance=Distance.COSINE))
    texts = ["Cats are small animals.", "Python is a programming language."]
    client.upsert(name, points=[
        PointStruct(id=i, vector=model.encode(t).tolist(), payload={"text": t})
        for i, t in enumerate(texts)
    ])
    hits = client.query_points(name, query=model.encode("What is Python?").tolist(), limit=1).points
    assert hits[0].payload["text"] == texts[1]
    client.delete_collection(name)
    client.close()
    print("[OK] Qdrant local mode works (insert + search).")


def check_ollama():
    r = httpx.post(f"{OLLAMA}/api/chat", timeout=120, json={
        "model": "llama3.2:3b",
        "messages": [{"role": "user", "content": "Reply with the single word: ready"}],
        "stream": False,
    })
    r.raise_for_status()
    print("[OK] Ollama replied:", r.json()["message"]["content"].strip())


def check_mysql():
    conn = pymysql.connect(**MYSQL)
    with conn.cursor() as cur:
        cur.execute("SELECT VERSION()")
        print("[OK] MySQL connected. Version:", cur.fetchone()[0])
    conn.close()


if __name__ == "__main__":
    failed = False
    for name, fn in [("MySQL", check_mysql), ("Ollama", check_ollama)]:
        try:
            fn()
        except Exception as e:
            failed = True
            print(f"[FAIL] {name}: {e}")
    try:
        m = check_embeddings()
        check_qdrant(m)
    except Exception as e:
        failed = True
        print(f"[FAIL] Embeddings/Qdrant: {e}")
    sys.exit(1 if failed else 0)