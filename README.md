# Projeto Bazel - Backend Java + Frontend Kotlin (JVM)

Este repositório é um monorepo usando **Bazel** para gerenciar:

- Backend em **Java**  
- Frontend em **Kotlin** compilado para JVM (aplicação desktop, servidor ou similar)

## Estrutura do projeto

```
creaite-app/
├── WORKSPACE.bazel
├── client/           # Frontend Kotlin JVM
│   ├── BUILD.bazel
│   └── src/
│       └── Main.kt
├── server/           # Backend Java
│   ├── BUILD.bazel
│   └── src/
│       └── Main.java
└── .gitignore
```

## Pré-requisitos

- Bazel instalado (recomendado >= 6.0)  
- JDK 17+ instalado  
- Kotlin configurado via regras Bazel (não precisa instalar manualmente)

## Como usar

### Rodar backend (Java)

```bash
bazel run //server:server_app
```

### Rodar frontend (Kotlin JVM)

```bash
bazel run //client:client_app
```

### Rodar testes (quando existirem)

```bash
bazel test //...
```

## Licença

Projeto aberto para uso livre.
