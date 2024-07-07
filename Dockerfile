FROM sbtscala/scala-sbt:eclipse-temurin-jammy-22_36_1.10.0_3.4.2

SHELL [ "/bin/bash", "--login", "-c" ]

# RUN apt-get update \
#   && apt-get install -y curl \
#   && apt-get -y autoclean

RUN curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash

ENV NODE_VERSION=22.4.0
ENV NVM_DIR=/root/.nvm

RUN source $NVM_DIR/nvm.sh \
  && nvm install $NODE_VERSION \
  && nvm alias default $NODE_VERSION \
  && nvm use default

ENV NODE_PATH $NVM_DIR/v$NODE_VERSION/lib/node_modules
ENV PATH $NVM_DIR/versions/node/v$NODE_VERSION/bin:$PATH

RUN node --version
RUN npm --version

WORKDIR /mlscript

COPY . .

RUN sbt "mlscriptJVM / test"

CMD [ "sbt" ]
