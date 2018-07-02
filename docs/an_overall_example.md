## 6. An overall Example

This example uses the Spark micro framework and implements a search over guardian
news articles. Be aware that this is just for demo purposes and therefor very limited.
Just browse the code or run the application and have fun ;)
To get it running an apiKey should be provided as parameter when running the java application. 

### 6.1 API

HOST: http://localhost:4567/

* GET /index : Indexes 500 latest news
* GET /search : Simple search
** q: The query string
** filter: The category filter (combined by 'or') *(multivalue)*
* GET /news : Ranked by scored date
** q: The query string
** p: The page number
** sort: score | date | scoredate
* GET /suggest
** q: The query string 
   