FROM sbtscala/scala-sbt:eclipse-temurin-alpine-21.0.2_13_1.10.0_3.4.2

WORKDIR /ucs

COPY . .

RUN sbt "npmBuildJS / Compile / fullLinkJS"

WORKDIR /ucs/web-demo

RUN npm install

RUN npm i -g serve

EXPOSE 3000

CMD [ "serve", "-s", "dist" ]
