.PHONY: test-backend test-frontend test-e2e test

# Backend: unit tests + Testcontainers integration tests
test-backend:
	cd backend && ./mvnw -B test

# Frontend: Vitest unit + component tests
test-frontend:
	cd frontend && npm ci --silent && npm test

# E2E: isolated docker stack + Playwright tests
test-e2e:
	docker compose -f compose.e2e.yml --profile e2e up -d --build
	bash -c 'while ! curl -sf http://localhost:5174/ > /dev/null; do sleep 1; done'
	cd frontend && npx playwright test
	docker compose -f compose.e2e.yml --profile e2e down -v

# Everything
test: test-backend test-frontend test-e2e
