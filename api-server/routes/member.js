'use strict'
//API모듈별로 분리하기 위해 라우터 사용. Express내부적으로 제공해주는 기능 
//회원 API 

// multipart/form-data 를 다루기 위한 node.js 의 미들웨어
const multer  = require('multer')
const upload = multer({ dest: 'uploads/' })
//파일경로를 읽기 위해 사용
const path = require("path");

//파일 읽기처리를 위해 사용
const fs = require('fs');
const { log } = require('console');

module.exports = (conn) => { 
const router = require('express').Router();
const connection = conn;


//mysql구문을 순차적으로 실행하기위해 사용하는 함수
function mySQLQuery(query) {
  return new Promise(function(resolve, reject) {
      try {
        connection.query(query.text, query.params, function(err, rows, fields) {
              if (err) {
                  return reject(err);
              } else {
                  //순차적으로 실행하면 반환되는 행을 관리
                  return resolve(rows);
              }
          });
      } catch (err) {
          return reject(err);
      }
  })
};



// 회원조회
router.get('/info/:id', (req, res) => {  
  const email = req.params.id;  
  const sql = 'SELECT mem_id, email, mem_name, profile, password FROM member WHERE email = ? ';

  connection.query(sql, email, (error, rows) => {

    let resultCode = 0;
    let message = '';

    if (error) {
      console.log(error);
      resultCode = 404;
      message = '에러가 발생했습니다';

    } else {

      if (rows[0]) { //회원인 경우
        resultCode = 200;
        message = '회원조회에 성공했습니다.';
      } else { //회원이 아닌 경우
        resultCode = 201;
        message = '가입되지 않은 회원입니다.';
      }    
    }
    
    res.json({
      'statusCode': resultCode,
      'message': message,
      'data':rows[0]
    });

  });

})

// 회원가입
router.post('/new', (req, res) => {
  const email = req.body.email;
  const name = req.body.name;
  const password = req.body.password;
  
  const sql = 'INSERT INTO member (email, password, mem_name, profile) VALUE (?, ?, ?, \'default_user.png\')';
  const params = [email, password, name];

  connection.query(sql, params, (error, rows) => {

    let statusCode = 404;
    let message = '에러가 발생했습니다';

    if (error) {
        console.log(error);
    } else {
      statusCode = 200;
      message = '회원가입에 성공했습니다.';
    }
    
    res.json({
      'statusCode': statusCode,
      'message': message      
    });
  })
    
})

//비밀번호 업데이트 
//1) 비밀번호 재발급 화면에서 요청 
//2) 비밀번호 변경화면에서 요청
router.post('/info/pwd', (req, res) => {

  const email = req.body.email;
  const pwd = req.body.password;
  console.log(pwd);

  const sql = 'UPDATE member SET password = ? WHERE email = ?';
  const params = [pwd, email];
I
  connection.query(sql, params, (error, rows) => {

    let statusCode = 404;
    let message = '에러가 발생했습니다';

    if (error) {
        console.log(error);
    } else {
      statusCode = 200;
      message = '업데이트에 성공했습니다.';
    }
    
    res.json({
      'statusCode': statusCode,
      'message': message      
    });
  });

})

//이름변경 요청 
router.post('/info/name', (req, res) => {

  const email = req.body.email;
  const name = req.body.name;
  console.log(name);

  const sql = 'UPDATE member SET mem_name = ? WHERE email = ?';
  const params = [name, email];

  connection.query(sql, params, (error, rows) => {

    let statusCode = 404;
    let message = '에러가 발생했습니다';

    if (error) {
        console.log(error);
    } else {
      statusCode = 200;
      message = '업데이트에 성공했습니다.';
    }
    
    res.json({
      'statusCode': statusCode,
      'message': message      
    });
  });

})
  
//uploads 경로에 파일을 저장하는 코드
//diskStorage - 파일을 디스크에 저장히기 위한 제어기능을 제공한다
const _storage = multer.diskStorage({
	destination: 'uploads/',
  filename: function (req, file, cb) {
    //확장자 반환
    const ext = path.extname(file.originalname);
    //경로의 마지막 부분을 반환
    cb(null, path.basename(file.originalname, ext) + ext);
  }
});

// 프로필사진 저장 API
// 클라이언트에서 받은 이미지를 파일로 저장하기 위해 multer 라이브러리 사용
// multer({storage: _storage}) : storage를 설정해, 파일을 저장하게 한다
// single : 지정된 form 필드와 연결된 단일 파일을 처리하는 미들웨어를 반환 (req.file로 전달되는 명)
router.post('/profile', multer({storage: _storage}).single('profile'), (req, res) => {
  const file = req.file;

  console.log('1. 이미지 업로드 요청 !!');
  
  try {
		let file = req.file;
		let originalName = '';
		let fileName = '';
		let mimeType = '';
		let size = 0;

		if(file) {
			originalName = file.originalname;
			filename = file.fileName;
			mimeType = file.mimetype;
			size = file.size;
		} else{
		}
	} catch (err) {
		console.dir(err.stack);
	}

  //let imgData = readImageFile(`./uploads/tempImg.png`)
  //let filePath =  '/uploads/' + req.file.originalname;
  
  //파일 확장자 
  const ext = path.extname(file.originalname);
  //파일저장 경로
  const filePath = path.basename(file.originalname, ext) + ext;  
  const email = req.body.email;

  //이전에 저장되었던 파일은 삭제 
  // - 만약, 이전에 저장된 프로필 사진이 있었다면, 
  // - 해당 사진은 삭제 하고, 새로 추가된 프로필 사진으로 업로드 

  let getBeforeProfile = 'SELECT profile FROM member WHERE email = ?;';
  connection.query(getBeforeProfile, email, (err, row) => {
    if(err) {
      console.log(err);
      res.json({
        'statusCode': 404,
        'message': '이미지 업로드 실패'      
      });
      return;
    }
      
    const result = row[0];
    //console.log('row[0]값 확인 : %o', row[0]);
    if(result.profile) { 
      console.log('2. 이전 프로필 사진 요청 처리 성공 !!!');

      //이전 profile값이 있을경우, 먼저 파일삭제 후 DB업데이트
      fs.unlink(`uploads/${result.profile}`, (err) => { 
        //console.log('이전 파일 삭제!');
        if(err) {
          console.error(err);
        }
      })
    }

    console.log('3. 이미지 업로드 요청 !!!');


     //filePath를 컬럼에 저장
     let sql = `UPDATE member SET profile = ? WHERE email = ?;`;

     connection.query(sql, [filePath, email], (err, rows, fields) => {
       if(err === null){
         //res.redirect("/uploads/" + req.file.originalname);
         console.log('4. 이미지 업로드 성공 !!! ');
         
         res.json({
           'statusCode': 200,
           'message': '이미지 업로드 성공'      
         });
   
   
       }else{
         console.log(String(err))
         //res.send("실패")
         res.json({
           'statusCode': 404,
           'message': '이미지 업로드 실패'      
         });
       }
     })

  });
})

//파일명을 파라미터로, 이미지 파일을 요청 
router.get('/profile/:filename', (req, res) => {
  const file = req.params.filename;
  
  fs.readFile('uploads/' + file, (err, data) => {
    //요청에 대한 응답헤더 설정
    res.writeHead(200, { 'Content-Type': 'image/jpeg'});
    res.end(data);
  });
})


//회원탈퇴처리
router.delete('/info/:id/:reasonnum', (req, res) => {
  const email = req.params.id;
  const reasonNum = req.params.reasonnum; //회원탈퇴사유 번호

  //아래쿼리 순차실행하여 회원탈퇴처리
  //1. 멤버정보 테이블에 탈퇴처리일, 탈퇴사유 저장
  //2. 해당 멤버가 작성한 회의예약내역들 삭제
  //3. 멤버테이블에서 회원정보 삭제하기

  const today = new Date();
  const leaveDate = today.toLocaleDateString(); //ex: 'yyyy. MM. dd.'
  //1.멤버정보 테이블에 탈퇴처리일, 탈퇴사유 저장
  const updateMemberManage = {
    text : 'INSERT INTO member_manage (leave_at, leave_cause_num) VALUE (?, ?)',
    params : [leaveDate, reasonNum],
  };

  //2.해당 멤버가 작성한 회의예약내역들 삭제
  const deleteMeetingById = {
    text : 'DELETE FROM meeting WHERE host_id = ?',
    params : [email],
  };

  //3.멤버테이블에서 회원정보 삭제하기
  const deleteMemberById = {
    text : 'DELETE FROM member WHERE email = ?',
    params : [email],
  };

  //쿼리들 순차 실행
  mySQLQuery(updateMemberManage)
    .then(mySQLQuery(deleteMeetingById))
    .then(mySQLQuery(deleteMemberById))
    .then(successCallback)
    .catch(errorCallback);

    //성공응답처리
    //병렬로 실행된 모든 행이 최종적으로 successCallback result매개변수에서 객체의 배열로 생성된다.
    function successCallback(result){
      //console.log('done',result);
      res.json({
        'statusCode': 200,
        'message': '회원탈퇴처리에 성공했습니다.'
      });
    }

    //error callback응답 처리
    function errorCallback(err){
      console.log('Error while executing SQL Query',err);
      res.json({
        'statusCode': 404,
        'message': '에러가 발생했습니다'
      });
    }

})


  return router;
}


