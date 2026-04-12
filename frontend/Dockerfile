FROM node:20-alpine AS builder

WORKDIR /app

# 패키지 설치
COPY package*.json ./
RUN npm install

# 소스 복사 및 빌드
COPY . .
RUN npm run build

# 2단계: 정적 파일을 serve로 제공
RUN npm install -g serve
CMD ["serve", "-s", "dist", "-l", "3003"]