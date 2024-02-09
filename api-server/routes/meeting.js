'use strict'
//API모듈별로 분리하기 위해 라우터 사용. Express내부적으로 제공해주는 기능 
//회의 API 
module.exports = (conn) => {

  const router = require('express').Router();
  const connection = conn; //mysql connection객체 전달

  //새회의 생성
  router.post('/new', (req, res) => {
    const title = req.body.title;
    const startDate = req.body.startDate;
    const startTime = req.body.startTime;

    // '2022-02-09 12:30:00' 포맷으로 변경
    const meeting_sdate = `${startDate} ${startTime}`;
    //const meeting_sdate = '2022-02-09 12:30:00'  
    const duration = req.body.duration;
    const gmt = req.body.gmt;

    const meeting_num = generateRandomCode(); //회의아이디, 6자리 번호
    const meeting_pwd = generateRandomPwd(); //6자리 문자와 숫자 포함

    const email = req.body.email;
    const hostId = req.body.hostId;
    const participantNum = req.body.participantNum;

    const sql = `INSERT INTO meeting (title, meeting_sdate, duration, gmt, meeting_num, meeting_pwd, email, host_id, participants_num) 
              VALUE (?, ?, ?, ?, ?, ?, ?, ?, ?)`;
    const params = [title, meeting_sdate, duration, gmt, meeting_num, meeting_pwd, email, hostId, participantNum];

    connection.query(sql, params, (error, rows) => {

      let statusCode = 404;
      let message = '에러가 발생했습니다';

      if (error) {
          console.log(error);
      } else {
        statusCode = 200;
        message = '회의추가에 성공했습니다.';
      }
    
      res.json({
        'statusCode': statusCode,
        'message': message,
        'meetingId': rows.insertId
      });
    });
  })

  //회의 리스트 호출 
  router.get('/list/:id', (req, res) => {

    const email = req.params.id;

    const sql = 'SELECT * FROM meeting WHERE email = ? order by meeting_sdate asc';

    connection.query(sql, email, (error, rows) => {

      let resultCode = 404;
      let message = '에러가 발생했습니다';

      if (error) {
          console.log(error);
      } else {
          resultCode = 200;
          message = '회의리스트 조회에 성공했습니다.';
      }    
      
      res.json({
        'statusCode': resultCode,
        'message': message,
        'data':rows
      });

    });
  })

  //회의리스트 페이징별 조회 API
  router.get('/list/:id/:limit/:page', (req, res) => {

    const email = req.params.id;
    const limit = req.params.limit;
    const page = req.params.page;
    const offset = (page - 1) * limit;
    
    const sql = 'SELECT *, (SELECT count(*) FROM meeting WHERE email = ? ) as totalCnt FROM meeting WHERE email = ? ORDER BY meeting_sdate ASC LIMIT ? OFFSET ?';

    const params = [email, email, Number(limit), Number(offset)];

    connection.query(sql, params, (error, rows) => {

      let resultCode = 404;
      let message = '에러가 발생했습니다';

      if (error) {
          console.log(error);
      } else {
          resultCode = 200;
          message = '회의리스트 조회에 성공했습니다.';
      }    

      let totalCnt = 0;//DB응답값이 없을 경우, 0개 리턴
      if (rows[0]) {
        totalCnt = rows[0].totalCnt;
      }

      
      res.json({
        'statusCode': resultCode,
        'message': message,
        'totalCnt': totalCnt,
        'data':rows
      });

    });
  })

  //회의ID로 회의 상세 조회
  router.get('/info/:id', (req, res) => {

    const meetingId = req.params.id;

    const sql = 'SELECT * FROM meeting WHERE meeting_id = ?';

    connection.query(sql, meetingId, (error, rows) => {

      let resultCode = 404;
      let message = '에러가 발생했습니다';

      if (error) {
          console.log(error);
      } else {
          resultCode = 200;
          message = '회의정보 조회에 성공했습니다.';
      }    
      
      res.json({
        'statusCode': resultCode,
        'message': message,
        'data':rows[0]
      });

    });
  })

  //오늘날짜 회의조회 
  router.get('/today/:date/:email/:limit/:page', (req, res) => {

    const date = req.params.date;
    const email = req.params.email;
    const limit = req.params.limit;
    const page = req.params.page;
    const offset = (page - 1) * limit;
    
    const params = [ date, email, date, email, Number(limit), Number(offset) ];    


    const sql = `select *, (SELECT count(*) FROM meeting where str_to_date(meeting_sdate, '%Y-%m-%d') = ? and email = ?) as totalCnt 
                  from meeting where str_to_date(meeting_sdate, '%Y-%m-%d') = ?
                    and email = ? ORDER BY meeting_sdate ASC LIMIT ? OFFSET ?;`;

    connection.query(sql, params, (error, rows) => {
      let resultCode = 404;
      let message = '에러가 발생했습니다';

      if (error) {
          console.log(error);
      } else {
          resultCode = 200;
          message = '회의정보 조회에 성공했습니다.';
      }
      
      let totalCnt = 0;
      if (rows[0]) {//값이 비었을경우, 0개 리턴
        totalCnt = rows[0].totalCnt;
      }
      
      res.json({
        'statusCode': resultCode,
        'message': message,
        'totalCnt': totalCnt,
        'data':rows
      });

    });

  })


  //회의번호로 회의존재유무 검증
  router.get('/verification', (req, res) => {

    //Response{protocol=http/1.1, code=404, message=Not Found, url=http://192.168.10.129:3001/api/meeting/verification/1231231/}
    
    const meeting_num = req.query.num;
    const meeting_pwd = req.query.pwd;

    //로그인유저 - 회의명만 전달 
    //미로그인유저 - 회의명과 비밀번호 전달
    let sql;
    let params;
    if (meeting_pwd) {
      sql = 'SELECT * FROM meeting WHERE meeting_num = ? AND meeting_pwd = ?';
      params = [meeting_num, meeting_pwd];

    } else {
      sql = 'SELECT * FROM meeting WHERE meeting_num = ?';
      params = [meeting_num];
    }
    
    connection.query(sql, params, (error, rows) => {

      let resultCode = 404;
      let message = '에러가 발생했습니다';

      if (error) {
          console.log(error);
      } else if (!rows[0]) {
          resultCode = 303;
          message = '일치하는 회의정보가 없습니다.';
      } else {
          resultCode = 200;
          message = '회의정보 조회에 성공했습니다.';
      }    
      
      res.json({
        'statusCode': resultCode,
        'message': message,
        'data': rows[0] || {}
      });
    })
  })

  //회의정보 수정
  router.post('/info', (req, res) => {

    const title = req.body.title;
    const startDate = req.body.startDate;
    const startTime = req.body.startTime;

    // '2022-02-09 12:30:00' 포맷으로 변경
    const meeting_sdate = `${startDate} ${startTime}`;
    //const meeting_sdate = '2022-02-09 12:30:00'  
    const duration = req.body.duration;
    const gmt = req.body.gmt; 
    const participantNum = req.body.participantNum;
    const meetingId = req.body.meetingId;

    const sql = `UPDATE meeting 
                    SET title = ?, meeting_sdate = ?, duration = ?, gmt = ?, participants_num = ?
                  WHERE meeting_id = ?`;

    const params = [title, meeting_sdate, duration, gmt, participantNum, meetingId];

    connection.query(sql, params, (error, rows) => {

      let statusCode = 404;
      let message = '에러가 발생했습니다';

      if (error) {
          console.log(error);
      } else {
        statusCode = 200;
        message = '회의수정에 성공했습니다.';
      }
      
      res.json({
        'statusCode': statusCode,
        'message': message      
      });
    });



  })

  //회의삭제 
  router.delete('/info/:id', (req, res) => {
    const meetingId = req.params.id;

    const sql = 'DELETE FROM meeting WHERE meeting_id = ? ';

    connection.query(sql, meetingId, (error, rows) => {

      let resultCode = 404;
      let message = '에러가 발생했습니다';

      if (error) {
          console.log(error);
      } else {
          resultCode = 200;
          message = '회의정보 삭제에 성공했습니다.';
      }    
      
      res.json({
        'statusCode': resultCode,
        'message': message
      });
    })
  })

  //오늘날짜 이전 회의는 삭제 
  router.delete('/yesterday/:date', (req, res) => {

    const yesterday = req.params.date;

    //mysql에선 delete나 update구분 처리시, 같은 테이블의 데이터를 곧바로 가져올 수 없기 때문에 
    //아래처럼 subQuery를 만들어서, 임시테이블의 결과로서 처리한다     
    const sql = `DELETE FROM meeting where meeting_id IN (
                  SELECT tbl1_alias.mid FROM ( 
                    SELECT meeting_id mid FROM meeting
                      WHERE meeting_sdate < ? ) tbl1_alias )`;

    connection.query(sql, yesterday, (error, rows) => {

      let resultCode = 404;
      let message = '에러가 발생했습니다';

      if (error) {
          console.log(error);
      } else {
          resultCode = 200;
          message = '오늘날짜 이전 회의정보 삭제에 성공했습니다.';
      }    
      
      res.json({
        'statusCode': resultCode,
        'message': message
      });
    })
  })

  
  //회의호스트가, 회의상태(close => open)변경
  router.post('/status', (req, res) => {
    const hostId = req.body.hostId;
    const meetingId = req.body.meetingId;

    const sql = `UPDATE meeting SET status = 'open' WHERE host_id = ? AND meeting_id;`;
    const params = [hostId, meetingId];

    connection.query(sql, params, (error, rows) => {

      let statusCode = 404;
      let message = '에러가 발생했습니다';

      if (error) {
          console.log(error);
      } else {
        statusCode = 200;
        message = '회의상태값 수정에 성공했습니다.';
      }
      
      res.json({
        'statusCode': statusCode,
        'message': message      
      });
    });
  })

  //회의ID 랜덤 생성
  function generateRandomCode() {
    let str = ''
    for (let i = 0; i < 7; i++) {
      str += Math.floor(Math.random() * 10)
    }
    return str
  }

  //회의랜덤 비밀번호 생성
  function generateRandomPwd () {
    const chars = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz'
    const stringLength = 6
    let randomstring = ''
    for (let i = 0; i < stringLength; i++) {
      const rnum = Math.floor(Math.random() * chars.length)
      randomstring += chars.substring(rnum, rnum + 1)
    }
    return randomstring
  }

  return router;
}


