# lms-system

## Backend (Spring Boot)

### Backend setup
```
cd lms-backend
mvn clean package
```

### Runs backend
```
java -jar target/lms-backend-0.0.1-SNAPSHOT.jar
```

Or run with dev profile:
```
java -jar target/lms-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

## Frontend (Vue)
```
npm install
```

### Compiles and hot-reloads for development
```
npm run serve:dev
```

### Compiles and minifies for production
```
npm run build
```

### Lints and fixes files
```
npm run lint
```

### Customize configuration
See [Configuration Reference](https://cli.vuejs.org/config/).
