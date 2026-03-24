import os
from huggingface_hub import hf_hub_download

MODEL_ID = "knowledgator/gliner-bi-base-v2.0"
TARGET_DIR = "/home/nickzt/Projects/LLM_campf/models_mnn/gliner-bi-v2"

print(f"Downloading tokenizer binaries for {MODEL_ID} to {TARGET_DIR}...")
try:
    hf_hub_download(repo_id=MODEL_ID, filename="spm.model", local_dir=TARGET_DIR)
    print("Downloaded spm.model successfully!")
except Exception as e:
    print("No spm.model found.", e)

try:
    hf_hub_download(repo_id=MODEL_ID, filename="tokenizer.model", local_dir=TARGET_DIR)
    print("Downloaded tokenizer.model successfully!")
except Exception as e:
    print("No tokenizer.model found.", e)

try:
    hf_hub_download(repo_id=MODEL_ID, filename="vocab.txt", local_dir=TARGET_DIR)
    print("Downloaded vocab.txt successfully!")
except Exception as e:
    print("No vocab.txt found.", e)
