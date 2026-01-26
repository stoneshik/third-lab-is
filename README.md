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

Сначала собираем jar файл при помощи скрипта<br>
`bash build.sh`

Затем поднимаем контейнеры<br>
`docker-compose up --build`

Остановка всех контейнеров<br>
`docker compose down`

Посмотреть запущенные сервисы<br>
`docker-compose ps`

Остановить конкретный сервис<br>
`docker-compose stop postgres`<br>
`docker-compose stop minio`<br>
`docker-compose stop app`

Запустить конкретный сервис<br>
`docker-compose start postgres`<br>
`docker-compose start minio`<br>
`docker-compose start app`

Web UI MinIO: http://localhost:9001<br>
login: minio<br>
password: minio123<br>
bucket: imports

Ссылка на фронтенд - https://github.com/stoneshik/third-lab-is-frontend

Писалось все по TDD, можно глянуть в гите (*≧ω≦*)
