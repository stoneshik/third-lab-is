# Информационные системы

## Лабораторная работа № 3

Для запуска тестов нужно задать значение переменной окружения `SPRING_PROFILES_ACTIVE` равное `test` - в этом профиле используется БД поднимаемая `testcontainers`

Для запуска в обычном режиме значение переменной окружения `SPRING_PROFILES_ACTIVE` равное `dev`

Файл `.env` должен находится в директории `resources`

Локальный файл .env, также есть файл .env.example

Подключение к серверу:<br>
`ssh s33xxxx@se.ifmo.ru -p 2222`

Проброс порта для helios:<br>
`ssh -L 8080:localhost:33xxxx s33xxxx@se.ifmo.ru -p 2222`

Url подключения к БД<br>
`jdbc:postgresql://localhost:5432/studs`

Сбор jar файла с пропуском тестов<br>
`mvn package -DskipTests`


Поднятие MinIO<br>
`docker compose up -d`

Остановка MinIO<br>
`docker compose down`

`minio-data` — именованный volume Docker
Физически лежит в:<br>
`/var/lib/docker/volumes/minio-data/_data`

Web UI MinIO: http://localhost:9001<br>
login: minio<br>
password: minio123<br>
bucket: imports

Ссылка на фронтенд - https://github.com/stoneshik/third-lab-is-frontend

Писалось все по TDD, можно глянуть в гите (*≧ω≦*)
