#docker run -d -p 9261:80 --name my-metrik -v "/Users/yxtan/Data/4KeyMetric/db:/data/db" -v "/Users/yxtan/Data/4KeyMetric/log:/app/logs" tanyixiu/my-metrik:latest
docker-compose -f stack.yml up -d