FROM sbtscala/scala-sbt:eclipse-temurin-jammy-22_36_1.10.0_3.4.2

USER sbtuser

RUN curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash

ENV NVM_DIR="$HOME/.nvm"

# [ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"  # This loads nvm
# [ -s "$NVM_DIR/bash_completion" ] && \. "$NVM_DIR/bash_completion"  # This loads nvm bash_completion

RUN "$NVM_DIR/nvm.sh"

RUN nvm install 22.4.0

RUN node --version

RUN npm --version

WORKDIR /mlscript

COPY . .

RUN sbt "mlscriptJVM / test"

CMD [ "sbt" ]
