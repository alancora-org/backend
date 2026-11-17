# Backend

Backend desenvolvido em Java 21, utilizando PostgreSQL, JDBC e autenticação JWT.

## Executando

É necessário ter **Java 21, Maven e PostgreSQL** instalados, além de um banco de dados já criado e em execução. Certifique-se de que o banco de dados esteja limpo antes de rodar o programa pela primeira vez.

1. Copie `.env.example` para `.env` (carregado automaticamente).
2. Configure as variáveis de ambiente conforme o exemplo, especialmente `DB_URL` e `JWT_SECRET`.
3. Execute dentro da pasta `backend/`:

```bash
mvn compile exec:java
```

A aplicação inicializa o esquema do banco automaticamente e disponibiliza a API em `http://localhost:8080`.
