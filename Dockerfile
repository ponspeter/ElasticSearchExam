FROM docker.elastic.co/elasticsearch/elasticsearch:8.17.0

LABEL authors="ponsp"

# Expose Elasticsearch ports
EXPOSE 9200 9300

# Set environment variables for single-node development
ENV discovery.type=single-node
ENV xpack.security.enabled=false