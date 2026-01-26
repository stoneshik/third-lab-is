mvn clean package -DskipTests
mkdir -p build
cp target/*.jar build/app.jar
