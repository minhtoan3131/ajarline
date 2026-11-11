# Postman demo

Demo the Ajarline API with Swagger UI and Postman.

## Run

1. Start PostgreSQL and the backend (see the project README). The API listens on http://localhost:8080.
2. In Postman, import `ajarline-api.postman_collection.json` and `ajarline-local.postman_environment.json`.
3. Select the environment `Ajarline · local`.
4. Open the environment and fill `username` and `password` in the **Value** column (called *Current value* in older Postman versions). Leave **Shared Value** empty.
5. Open the folder `Demo / V02 · Article lifecycle` and run it (Runner), or send each request in order.

Swagger UI: http://localhost:8080/swagger-ui.html