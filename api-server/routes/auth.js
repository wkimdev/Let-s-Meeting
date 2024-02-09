'use strict'
//API모듈별로 분리하기 위해 라우터 사용. Express내부적으로 제공해주는 기능 
//회원인증 API 
module.exports = (conn) => { 
const router = require('express').Router();
const connection = conn;
//로그인비밀번호 decript를 하기위한 모듈사용
const CryptoJS = require("crypto-js");


//로그인
router.post('/member', (req, res) => {
  const {email, password} = req.body;

  const sql = 'SELECT mem_id, email, mem_name, profile FROM member WHERE email = ? AND password = ?';
  const params = [email, password];

  connection.query(sql, params, (error, rows) => {

    let statusCode = 404;
    let message = '에러가 발생했습니다';

    if (error) {
        console.log(error);
    }
    
    if (!rows[0]) {
      statusCode = 201;
      message = '잘못된 이메일 또는 전화번호값.';
    } else {
      statusCode = 200;
      message = '로그인에 성공했습니다.';
    }
    
    //res.header("Access-Control-Allow-Origin", "*");
    res.json({
      'statusCode': statusCode,
      'message': message,
      'data':rows[0]
    });

  });

})
  return router;
}