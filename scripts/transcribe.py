import sys
import os
import requests
import json

def transcribe(audio_path, base_url, api_key, model_name):
    url = f"{base_url}/audio/transcriptions"
    headers = {"Authorization": f"Bearer {api_key}"}
    files = {"file": open(audio_path, "rb")}
    data = {"model": model_name}

    try:
        response = requests.post(url, headers=headers, files=files, data=data)
        response.raise_for_status()
        return response.json().get("text", "")
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        return ""

if __name__ == "__main__":
    if len(sys.argv) < 5:
        print("Usage: transcribe.py <audio_path> <base_url> <api_key> <model_name>")
        sys.exit(1)
    
    path = sys.argv[1]
    url = sys.argv[2]
    key = sys.argv[3]
    model = sys.argv[4]
    
    text = transcribe(path, url, key, model)
    print(text)
