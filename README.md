📌 User API Documentation
📍 Base URL: http://localhost:8080

🧑‍💻 Criar Usuário
Endpoint: /users
Método: POST
Descrição: Cria um novo usuário no sistema
📥 Request Body
{
  "name": "Thales Assis",
  "cpf": "12345678900",
  "genre": "MASCULINO",
  "birthDate": "2000-01-10T00:00:00.000+00:00",
  "email": "thales@email.com",
  "password": "123456",
  "profile": "PACIENTE",
  "address": {
    "streetName": "Rua dos bobos",
    "streetNumber": "número 0",
    "complement": "casa amarela",
    "city": "Curitiba",
    "state": "PR",
    "zipCode": "00000000"
  }
}

✏️ Atualizar Usuário
Endpoint: /users/{id}
Método: PUT
Descrição: Atualiza os dados de um usuário existente
📥 Request Body
{
  "name": "Thales Oliveira",
  "cpf": "1111111111",
  "genre": "FEMININO",
  "birthDate": "2000-01-10T00:00:00.000+00:00",
  "email": "thales@email.com",
  "password": "123456",
  "profile": "PROFISSIONAL",
  "address": {
    "streetName": "Rua Mauá",
    "streetNumber": "número 10",
    "complement": "casa verde",
    "city": "Curitiba",
    "state": "PR",
    "zipCode": "11111111111"
  }
}
🔍 Buscar Usuário por ID
Endpoint: /users/{id}
Método: GET
Descrição: Retorna os dados de um usuário específico
📥 Request Body
Vazio
📋 Listar Todos os Usuários
Endpoint: /users/all
Método: GET
Descrição: Retorna todos os usuários cadastrados
📥 Request Body
Vazio
❌ Deletar Usuário
Endpoint: /users/{id}
Método: DELETE
Descrição: Remove um usuário do sistema com base no ID
📥 Request Body
Vazio
📌 Observações
O campo birthDate deve estar no formato ISO 8601
O campo cpf deve conter apenas números
O campo profile pode assumir valores como:
PACIENTE
PROFISSIONAL
ADMIN
O campo genre pode assumir valores como:
MASCULINO
FEMININO
PREFIRO_NAO_INFORMAR
OUTRO
