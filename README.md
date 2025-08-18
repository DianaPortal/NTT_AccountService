# Customers Service - NTT Data Bootcamp (Avance 1)

Microservicio bancario desarrollado en el Bootcamp de Microservicios.
Este repositorio corresponde al Avance 1 del proyecto, donde se implementa el microservicio de cuentas.


# Endpoints (CRUD)

Base URL: http://localhost:8085/api/accounts

POST /api/accounts – Crea cuenta (SAVINGS | CHECKING | FIXED_TERM)

GET /api/accounts – Lista todas

GET /api/accounts/{{id}} – Busca por id

PUT /api/accounts/{{id}} – Actualiza

DELETE /api/accounts/{{id}} – Elimina


# Diagramas UML
Muestra la interacción entre Clientes, Transacciones, Cuentas y Créditos.
![](https://github.com/user-attachments/assets/288b7378-24f4-4be6-97f2-167f06baee26)


# Diagrama de Secuencia CRUD Cuentas

<img width="981" height="1315" alt="image" src="https://github.com/user-attachments/assets/0e064823-14be-482d-917a-35402bed412f" />
