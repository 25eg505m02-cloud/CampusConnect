# Container seed data

The JSON files in this directory are the container-build copy of the canonical fixtures in the repository-root `collections/` directory. Keep the two directories in sync when changing seed data; the backend Docker build context is `backend/`, so Docker cannot copy files from its parent directory.

The seed loader reads this directory in the deployed container as `/app/collections`.
