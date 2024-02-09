
const express = require('express')
const app = express()
//mysql 연동을 위해 모듈을 가져온다
const mysql = require('mysql');
//서버에서 CORS 허용을 하기 위한 모듈
const cors = require('cors');

// POST요청시 들어오는 BODY를 처리하기 위해 body-parser 설정
const bodyParser = require('body-parser');
const req = require('express/lib/request');


// Add cors
app.use(cors());
app.options('*', cors());  // enable pre-flight
//app.use(bodyParser.json());

// const whitelist = ['*'];

// const corsOptions = {
//   origin: function(origin, callback){
//   var isWhitelisted = whitelist.indexOf(origin) !== -1;
//   callback(null, isWhitelisted); 
//   // callback expects two parameters: error and options 
//   },
//   credentials:true
// }

// app.use(cors(corsOptions)); //CORS 미들웨어등록


//body-parser 설정
app.use(bodyParser.urlencoded({extended:true})); // request 객체의 body에 대한 url encoding의 확장을 할 수 있도록 하는 설정
app.use(bodyParser.json()); //request body에 오는 데이터를 json 형식으로 변환



//서버포트 지정
const port = 3001;

//mysql 연동 설정
const connection = mysql.createConnection({
    host: "192.168.10.129",
    user: "root",
    database: "letsmeeting",
    password: "password",
    port: 3306
});

app.get('/', (req, res) => {
  res.send('Hello World!')
})

//라우터모듈 사용
//require로 해당 라우터 모듈을 가져오고, 
//app.use('/경로', 라우터)로 라우터 사용설정 
//'/경로'로 해당하는 작업을 라우터가 실행한다
const auth = require('./routes/auth')(connection);
app.use('/api/auth', auth);

const member = require('./routes/member')(connection);
app.use('/api/member', member);

const meeting = require('./routes/meeting')(connection);
app.use('/api/meeting', meeting);


app.listen(port, () => {
  console.log(`Example app listening on port ${port}`)
});

