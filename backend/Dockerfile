# Imagem da aplicacao, usada para publicar o sistema na nuvem.
#
# Sao duas etapas: a primeira compila o projeto com o Maven e a
# segunda copia so o .jar gerado para uma imagem menor, que nao
# precisa ter o Maven nem o codigo fonte dentro.

# ---------- etapa 1: compilar ----------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copiamos primeiro so o pom.xml e baixamos as dependencias.
# Assim, quando so o codigo muda, o Docker reaproveita esta camada
# e o build fica bem mais rapido.
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# ---------- etapa 2: executar ----------
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/controle-estoque-0.1.0.jar app.jar

# O servidor gratuito tem pouca memoria, entao limitamos a JVM
# para ela nao tentar usar mais do que existe.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70"

EXPOSE 8080

CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
