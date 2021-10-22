FROM maven:3.8.3-openjdk-17-slim AS builds
WORKDIR /builds/hnc
COPY . .
RUN mvn package 

FROM openjdk:17-slim
WORKDIR /apps/hnc
RUN groupadd hnc && useradd -m -g hnc hnc
USER hnc:hnc
RUN mkdir /home/hnc/logs
COPY --from=builds /builds/hnc/target/HiranaNetworkConnection.jar /apps/hnc
ENTRYPOINT [ "java", "-jar", "/apps/hnc/HiranaNetworkConnection.jar" ]