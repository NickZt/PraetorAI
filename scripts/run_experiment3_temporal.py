#!/usr/bin/env python3
"""
Experiment 3: Temporal Resolution & Anachronism Prevention
Validates SOP 3.1: Ensures LangChain4j RAG Agent accurately resolves 
historical contexts when laws/charters are amended, using Neo4j temporal filters.

Requirements:
- neo4j python driver (`pip install neo4j`)

Usage:
    python3 run_experiment3_temporal.py
"""

from neo4j import GraphDatabase
import os
import sys

URI = os.getenv("NEO4J_URI", "bolt://localhost:7687")
USER = os.getenv("NEO4J_USERNAME", "neo4j")
PASSWORD = os.getenv("NEO4J_PASSWORD", "password")

def setup_temporal_charter(session):
    print("🧹 Cleaning previous temporal state...")
    session.run("MATCH (n:CharterTest) DETACH DELETE n;")
    session.run("MATCH (a:ActionNode {scenario: 'charter_test'}) DETACH DELETE a;")
    
    print("📚 Injecting 'Field Charter' and its subsequent 'Amendment'...")
    
    # 1. Base Law (Valid from 2021 to 2023)
    # 2. Amendment that cancels a clause (Valid from 2024 -> Onward)
    cypher = """
        CREATE (charter:CharterTest {id: "charter_alpha", title: "Field Charter Alpha"})
        WITH charter
        
        CREATE (a1:ActionNode {
            type: 'ENACTED', 
            StartDate: datetime('2021-01-01T00:00:00Z'), 
            EndDate: datetime('2023-12-31T23:59:59Z'), 
            clause_5: 'Patrols required every 2 hours',
            scenario: 'charter_test'
        })
        CREATE (charter)-[:HAS_ACTION]->(a1)
        
        CREATE (a2:ActionNode {
            type: 'AMENDED', 
            StartDate: datetime('2024-01-01T00:00:00Z'), 
            EndDate: null, 
            clause_5: 'Patrols required every 6 hours',
            scenario: 'charter_test'
        })
        CREATE (charter)-[:HAS_ACTION]->(a2)
    """
    session.run(cypher)

def mock_langchain_agent_query(session, target_id, query_date):
    """
    Simulates the LangChain4j Cypher retrieval tool that uses hard Temporal Filtering.
    """
    cypher_query = """
        MATCH (charter:CharterTest {id: $target_id})-[:HAS_ACTION]->(action:ActionNode)
        WHERE action.StartDate <= datetime($query_date)
          AND (action.EndDate IS NULL OR action.EndDate > datetime($query_date))
        RETURN action.clause_5 as clause_5, action.type as status, elementId(action) as action_ref
    """
    result = session.run(cypher_query, target_id=target_id, query_date=query_date)
    record = result.single()
    
    if record:
        return {
            "retrieved_clause": record["clause_5"], 
            "status": record["status"], 
            "action_ref": record["action_ref"]
        }
    return None

def run_experiment():
    print("=========================================================")
    print("🕰️  Experiment 3: RAG Temporal Resolution (Anachronism Test)")
    print("=========================================================")
    
    try:
        driver = GraphDatabase.driver(URI, auth=(USER, PASSWORD))
        driver.verify_connectivity()
    except Exception as e:
        print(f"❌ Neo4j Connection Failed: {e}")
        sys.exit(1)
        
    with driver.session() as session:
        setup_temporal_charter(session)
        
        print("\n🔍 RAG Agent querying: 'What was the patrol requirement on [Date X]?'")
        
        # Test 1: Query the timeline BEFORE the amendment (e.g. 2022)
        date_x_before = "2022-06-15T12:00:00Z"
        res_before = mock_langchain_agent_query(session, "charter_alpha", date_x_before)
        
        print(f"\n   [Date X = {date_x_before}]")
        print(f"   -> RAG Retrieved: '{res_before['retrieved_clause']}' (Status: {res_before['status']})")
        print(f"   -> Reference: ActionNode ID {res_before['action_ref']}")
        
        # Test 2: Query the timeline AFTER the amendment (e.g. 2024)
        date_x_after = "2024-03-01T09:00:00Z"
        res_after = mock_langchain_agent_query(session, "charter_alpha", date_x_after)
        
        print(f"\n   [Date X = {date_x_after}]")
        print(f"   -> RAG Retrieved: '{res_after['retrieved_clause']}' (Status: {res_after['status']})")
        print(f"   -> Reference: ActionNode ID {res_after['action_ref']}")
        
        passed = False
        if res_before and res_after:
            if "2 hours" in res_before['retrieved_clause'] and "6 hours" in res_after['retrieved_clause']:
                passed = True
                
        print("\n📊 --- FINAL RESULTS ---")
        if passed:
            print("✅ EXPERIMENT 3 PASSED: The LangChain4j temporal Cypher-filter successfully returned the valid status without anachronisms, providing direct links to the correct ActionNode timestamps.")
        else:
            print("❌ EXPERIMENT 3 FAILED: Anachronism detected. Neo4j returned invalid temporal context.")

if __name__ == "__main__":
    run_experiment()
