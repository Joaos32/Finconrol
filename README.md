# FinControl

FinControl é um sistema de gestão financeira pessoal em monólito modular, com API Spring Boot e interface Angular. O projeto cobre a V1 e iniciou a V2 com orçamentos mensais por categoria.

## Funcionalidades

- Cadastro e login com JWT.
- Dados financeiros isolados por usuário autenticado.
- CRUD de contas, categorias e transações.
- Paginação e filtros por período, conta, categoria e tipo.
- Saldo e indicadores do dashboard calculados a partir das transações.
- Orçamentos mensais por categoria de despesa, com gasto, valor disponível e alerta de limite excedido.
- Migrations versionadas com Flyway.

## Tecnologias

- Java 21, Spring Boot 4.1.x, Maven, Spring Security, Spring Data JPA, PostgreSQL, Flyway, MapStruct, OpenAPI.
- Angular 22, TypeScript, Angular Material, Reactive Forms, RxJS e SCSS.
- JUnit, Mockito e Testcontainers para testes.

## Arquitetura

Monólito modular organizado por domínio (auth, user, account, category, transaction e dashboard). Controllers cuidam do contrato HTTP; services concentram regras de negócio; repositories limitam leituras ao usuário autenticado. DTOs são mapeados com MapStruct. O banco é atualizado exclusivamente por migrations Flyway.

## Como executar com Docker

1. Copie .env.example para .env.
   Gere um `JWT_SECRET` aleatorio de 48 bytes no PowerShell: `[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))`.
2. Defina POSTGRES_PASSWORD e JWT_SECRET. Use um segredo aleatório de pelo menos 32 bytes para JWT_SECRET.
3. Execute docker compose up --build.
4. API: http://localhost:8080; Swagger: http://localhost:8080/swagger-ui.html; health: http://localhost:8080/actuator/health.
5. Para incluir o frontend: docker compose --profile fullstack up --build.

O perfil dev cria o usuário de demonstração (demo@fincontrol.dev, senha definida por DEMO_PASSWORD, por padrão Demo@123) com duas contas, categorias e transações de exemplo. Esse seed não é carregado no perfil prod.

## Execução local

Requisitos: JDK 21, Maven 3.9+, Node.js compatível com Angular 22 (24.15+), npm e PostgreSQL 17.

Configure as variáveis do .env.example no ambiente, inicie PostgreSQL, depois execute mvn spring-boot:run em backend/. Em frontend/, execute npm install e npm start. A interface usa /api no mesmo host; localmente, o proxy Angular encaminha esse caminho para http://localhost:8080.

## Variáveis de ambiente

DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD, JWT_SECRET, JWT_EXPIRATION, CORS_ALLOWED_ORIGINS e DEMO_PASSWORD. Não versione .env ou segredos reais.

O MVP guarda o access token em sessionStorage, que é limpo ao fechar a sessão do navegador. Uma evolução pode migrar a autenticação para cookie HttpOnly, Secure e SameSite, com proteção CSRF apropriada.

## API

- POST /api/auth/register, POST /api/auth/login
- /api/accounts e /api/accounts/{id}/balance
- /api/categories
- /api/transactions com paginação e filtros
- /api/budgets?month=AAAA-MM para consultar orçamentos do mês; POST, PUT e DELETE em /api/budgets
- /api/dashboard/summary, /expenses-by-category, /monthly-evolution e /recent-transactions
- /v3/api-docs e /swagger-ui.html

As rotas financeiras exigem Authorization: Bearer <token>. O backend obtém a identidade do JWT; nenhuma requisição recebe userId para definir propriedade.

## Testes

Em `frontend/`, execute `npm ci`, `npm audit` e `npm run build`. O aceite de navegador usa Playwright: instale o Chromium com `npx playwright install chromium` e execute `npm run e2e` com a API em `http://localhost:8080` e o Angular em `http://localhost:4200`. O GitHub Actions sobe PostgreSQL e os dois aplicativos para executar esse fluxo completo.

Em backend/, execute mvn test. Testes de integração PostgreSQL usam Testcontainers e são ignorados automaticamente quando o Docker não está disponível.

## Roadmap

- V1: contas, categorias, receitas, despesas e dashboard.
- V2: orçamentos mensais por categoria (implementado); cartões, faturas, parcelamentos, recorrências e metas.
- V3: relatórios, importação de extratos e insights.
- V4: Redis, RabbitMQ, AWS e observabilidade.
- V5: recursos de IA financeira.
