FROM sphinxdoc/sphinx

ARG PORT
ENV PORT=$PORT
EXPOSE $PORT/tcp

COPY . .
RUN apt-get update && apt-get install -y make
RUN pip install --no-cache-dir sphinx-rtd-theme
RUN make clean html

CMD python -m http.server -d _build/html $PORT
