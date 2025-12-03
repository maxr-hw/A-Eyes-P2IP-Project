import requests
import json

import time

def call_replicate_model(api_token, version_url, inputs):
    headers = {
        "Authorization": f"Bearer {api_token}",  # <- FIXED HERE
        "Content-Type": "application/json",
    }

    data = {
        "version": version_url,
        "input": inputs
    }

    response = requests.post(
        "https://api.replicate.com/v1/predictions",
        headers=headers,
        json=data
    )

    response.raise_for_status()
    prediction = response.json()

    # Poll until prediction is complete
    status_url = prediction["urls"]["get"]
    while prediction["status"] not in ["succeeded", "failed", "canceled"]:
        time.sleep(2)
        prediction = requests.get(status_url, headers=headers).json()

    if prediction["status"] == "succeeded":
        return prediction["output"]
    else:
        return {"error": prediction.get("error", "Prediction failed")}


def upload_image(file_path):
    with open(file_path, 'rb') as f:
        response = requests.post(
            "https://transfer.sh/image.jpg",
            files={"file": f}
        )
    if response.status_code == 200:
        return response.text.strip()
    else:
        raise Exception(f"Upload failed: {response.status_code}")

def analyze_image_VQA(version_url, api_token, image_path, prompt):
    try:
        print(f"Image path: {image_path}")
        print(f"Prompt: {prompt}")

        inputs = {
            "image": image_path,
            "task": "visual_question_answering",
            "question": prompt
        }
        return call_replicate_model(api_token, version_url, inputs)
    except Exception as e:
        import traceback
        traceback.print_exc()
        return {"error": str(e)}

def describe_object_in_image(version_url, api_token, image_path):
    inputs = {
        "image": image_path,
        "task": "caption"
    }
    return call_replicate_model(api_token, version_url, inputs)

def describe_scenery_in_image(version_url, api_token, image_path):
    inputs = {
        "image": image_path,
        "task": "caption"
    }
    return call_replicate_model(api_token, version_url, inputs)