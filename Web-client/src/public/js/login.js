'use strict'

//로그인비밀번호 암호화를 위한 모듈
//const sha256 = require('js-sha256');
const btnLogin = document.getElementById('btnLogin');
const userId = document.getElementById('userId');
const pwd = document.getElementById('password');

const loginContainer = document.querySelector('.uk-position-center.login > div');
const container = document.querySelector('.uk-position-center.login');
const saveIdCheckbox = document.querySelector('#saveId__checkbox');
//현재날짜
const currentTime = document.querySelector('.currentTime');

import { API_SERVER_URL } from './config.js'
//const API_SERVER_URL = 'http://192.168.10.129:3001';


init();

window.addEventListener('load', () => {
  console.log('#### 222. load evnet!....');
  currentTime.innerText = getCurrentDate();
});

//로그인화면 진입시 최초로 실행되는 함수 -> 더 빨리 차단해야하는데
//init()

function init() {

  //로그인상태면 홈으로 이동'
  // const isLogin = localStorage.getItem('isLogin');
  // console.log('#### load evnet! isLogin... %o', isLogin);
  // if ( isLogin === 'true') {
  //   console.log('#### 로그인화면 로그인상태 OK!....');
  //   return location.replace(`${window.location.origin}/`);
  // }

  //아이디저장을 체크한 상태면 아이디를 인풋박스에 미리 불러와 셋팅해놓고
  //아이디 저장 체크박스를 미리 활성화 시켜놓는다.
  if (localStorage.getItem('isSaveUserId') === 'true') {
    userId.value = localStorage.getItem('email');
    saveIdCheckbox.checked = true;
  } else {
    saveIdCheckbox.checked = false;
  }
}

// setUserId();

// function setUserId() {
// }

function getCurrentDate() {
  const today = new Date();
  const year = today.getFullYear();
  const month = ('0' + (today.getMonth() + 1)).slice(-2);
  const day = ('0' + today.getDate()).slice(-2);
  return `${year}년 ${month}월 ${day}일`;
}


//로그인 실행
btnLogin.addEventListener('click',()=>{

  const id = userId.value;
  const password = pwd.value;

  //유효성 빈값 체크
  if (!id) {
    userId.focus();
    alert("로그인ID를 입력해주세요!");
    return;
  }

  if (!password) {
    pwd.focus();
    alert("비밀번호를 입력해주세요!");
    return;
  }

  
  const hashPwd = sha256(password);
  //password 암호화
  const parameter = {
      email:id,
      password:hashPwd
  };

  /**
   * API서버에 로그인요청
   * -> 한 도메인에서 다른도메인으로 요청시
   * 요청설정에 CORS설정이 되어 있어야 하고
   * 수신도메인은(API서버) 요청 도메인을 허용시켜놓아야 한다.
   */
  const url = `${API_SERVER_URL}/api/auth/member`;
  console.log(url);
  fetch(url, {
    method: 'POST',
    mode: 'cors', //해당 옵션을 주지 않으면, body객체가 서버로 전달되지 않는다.
    headers: { //JSON Type으로 보내기위한 헤더설정
      'Accept': 'application/json',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(parameter),
  }).then(res => res.json())
  .then(response => {
    const responseData = response.data;
    
    if (response.statusCode === 200) {//로그인성공
      
      const email = responseData.email;  
      localStorage['isLogin'] = true;
      localStorage['profile'] = responseData.profile;
      localStorage['memName'] = responseData.mem_name;
      
      //아이디 저장여부가 체크되었을때 로컬스토리지에 아이디저장 되도록 처리
      if (saveIdCheckbox.checked) {
        /**
         * @TODO
         * 쿠키/세션처리로 변경되어야함!
         * 쿠키:USERID를 한달간저장 
         * 세션:로그인상태유지 6시간. 로그인정보(유저ID, 이메일, 프로필사진, 닉네임)를 세션에 넣는다.
         */
        //setCookie(response.data.email);
        //체크박스 아이디저장값 활성화
        localStorage['isSaveUserId'] = true;
        localStorage['email'] = email;
      } else {
        //로그인상태유지 체크 해지하면
        //쿠키를 삭제한다.
        // localStorage.clear();

        //체크박스 아이디저장값 비활성화
        localStorage['isSaveUserId'] = false;
        localStorage['email'] = '';
      }
      //로그인상태 업데이트
      //페이지 홈으로 이동
      location.href = `${window.location.origin}/login/state?email=${email}`;

    } else {
      //로그인실패 
      alert('로그인 아이디 또는 비밀번호를 확인해주세요!');
      localStorage['isLogin'] = false;
      pwd.value = '';
      return;
    }
  })
  .catch(error => console.error('Error:', error));

});

//로그인ID를 한달간 쿠키에 저장한다.
// (time() + 86400 * 30)
// function setCookie(userId) {
//   browser.cookies.set({
//     expirationDate: , //한달동안 로그인 유저ID저장-> 초(second)로 작성해야함
//     name: "userId",
//     value: userId
//   });
// }

//로그인ID를 한달간 쿠키에 저장한다.
function setCookie(userId) {
  const now = new Date();
  now.setMonth( now.getMonth() + 1 );

  document.cookie="userId=" + userId + ";";
  
  // document.cookie = "expires=" + now.toUTCString() + ";"
}

//쿠키를 삭제하는 코드
function delete_cookie( name, path, domain ) {
  if( get_cookie( name ) ) {
    document.cookie = name + "=" +
      ((path) ? ";path="+path:"")+
      ((domain)?";domain="+domain:"") +
      ";expires=Thu, 01 Jan 1970 00:00:01 GMT";
  }
}

//쿠키를 호출하는 코드
function get_cookie(name){
  return document.cookie.split(';').some(c => {
      return c.trim().startsWith(name + '=');
  });
}