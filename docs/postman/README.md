# Postman demo

Demo the Ajarline API with Swagger UI and Postman.

## Run

1. Start PostgreSQL and the backend (see the project README). The API listens on http://localhost:8080.
2. In Postman, import `ajarline-api.postman_collection.json` and `ajarline-local.postman_environment.json`.
3. Select the environment `Ajarline · local`.
4. Open the environment and fill `username` and `password` in the **Value** column (called *Current value* in older Postman versions). Leave **Shared Value** empty. If you keep them in the Postman Vault, unlock the Vault before you run a folder.
5. Open the folder `Demo` and run `V02 · Article lifecycle`, `V03 · Browse without Search` or `V04 · Learn through a Series` (Runner), or send each request in order.
6. To run a folder again, change the slug variable it uses (`slug` for `V02`, `articleSlug` for `V03`, `seriesSlug` for `V04`): an Article, a Module or a Series cannot be deleted, so its slug stays taken.

Swagger UI: http://localhost:8080/swagger-ui.html