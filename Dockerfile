FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY Main.java .
COPY index.html .
COPY style.css .

RUN javac Main.java

CMD ["java", "Main"]