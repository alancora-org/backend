# Backend

Backend desenvolvido em Java 21, utilizando PostgreSQL, JDBC e autenticação JWT.

## Executando

É necessário ter **Java 21, Maven e PostgreSQL** instalados.

**O serviço PostgreSQL deve estar em execução e o banco usado pela aplicação deve existir e estar vazio antes da primeira execução, sem tabelas nem dados anteriores.** O backend cria as tabelas, mas não inicia o PostgreSQL nem cria o banco, apenas as tabelas.

1. Copie `.env.example` para `.env` (carregado automaticamente).
2. Configure as variáveis de ambiente conforme o exemplo, especialmente `DB_URL` e `JWT_SECRET`.
3. Execute dentro da pasta `backend/`:

```bash
mvn compile exec:java
```

A aplicação inicializa o esquema do banco automaticamente e disponibiliza a API em `http://localhost:8080`.
