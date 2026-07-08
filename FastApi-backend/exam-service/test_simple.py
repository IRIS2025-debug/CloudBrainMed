from fastapi import FastAPI

app = FastAPI()

@app.get("/")
async def root():
    return {"status": "ok"}

@app.get("/test")
async def test():
    return {"message": "test works"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8006)