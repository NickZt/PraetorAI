import requests
import json
import time
from neo4j import GraphDatabase
import os

class IngestorClient:
    def __init__(self, base_url="http://localhost:8081"):
        self.base_url = base_url

    def ingest_file(self, file_path, metadata=None):
        url = f"{self.base_url}/ingest"
        payload = {
            "path": file_path,
            "metadata": metadata or {}
        }
        response = requests.post(url, json=payload, timeout=10.0)
        response.raise_for_status()
        return response.json()

    def get_status(self, job_id):
        url = f"{self.base_url}/v1/ingest/status/{job_id}"
        response = requests.get(url, timeout=5.0)
        response.raise_for_status()
        return response.json()

    def wait_for_completion(self, job_id, timeout=60, interval=2):
        start_time = time.time()
        while time.time() - start_time < timeout:
            status = self.get_status(job_id)
            if status.get("status") == "COMPLETED":
                return status
            if status.get("status") == "FAILED":
                raise Exception(f"Ingestion failed: {status.get('error')}")
            time.sleep(interval)
        raise TimeoutError(f"Ingestion timed out after {timeout} seconds")

class AssistantClient:
    def __init__(self, base_url="http://localhost:8081"):
        self.base_url = base_url

    def query(self, query_text):
        url = f"{self.base_url}/query"
        payload = {
            "query": query_text
        }
        response = requests.post(url, json=payload, timeout=30.0)
        if response.status_code != 200:
            try:
                error_info = response.json()
                raise Exception(f"HTTP {response.status_code}: {error_info.get('error', 'Unknown Error')}")
            except:
                response.raise_for_status()
        return response.json()

class GraphValidator:
    def __init__(self, uri=None, user=None, password=None):
        self.uri = uri or os.getenv("NEO4J_URI", "bolt://localhost:7687")
        self.user = user or os.getenv("NEO4J_USERNAME", "neo4j")
        self.password = password or os.getenv("NEO4J_PASSWORD", "password")
        self._driver = GraphDatabase.driver(self.uri, auth=(self.user, self.password))

    def close(self):
        self._driver.close()

    def query(self, cypher, params=None):
        with self._driver.session() as session:
            result = session.run(cypher, params or {})
            return [record.data() for record in result]

    def verify_node_exists(self, label, properties, timeout=30):
        props_str = " AND ".join([f"n.{k} = ${k}" for k in properties.keys()])
        cypher = f"MATCH (n:{label}) WHERE {props_str} RETURN count(n) as count"
        
        start_time = time.time()
        while time.time() - start_time < timeout:
            results = self.query(cypher, properties)
            if results and results[0]['count'] > 0:
                return True
            time.sleep(2)
        
        # Debug info if not found
        try:
            print(f"      [Validator Debug]: Node {label} with {properties} not found.")
            all_labels = self.query("MATCH (n) RETURN DISTINCT labels(n) as labels")
            print(f"      [Validator Debug]: Available labels in DB: {all_labels}")
            if label:
                sample = self.query(f"MATCH (n:{label}) RETURN n LIMIT 1")
                print(f"      [Validator Debug]: Sample {label} node: {sample}")
        except Exception as e:
            print(f"      [Validator Debug]: Error fetching debug info: {e}")
            
        return False
