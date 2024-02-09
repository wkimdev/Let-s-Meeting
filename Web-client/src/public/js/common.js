'use strict';

/** Navbar, footer공통 처리를 하기 위한 JS */
//네비게이션바
const beforeLogin = document.querySelector('.before__login');
const profileImg = document.querySelector('.profile_img');
const afterLogin = document.querySelector('.after__login');

//로그인시 서브 네비게이션바
const welcomeText = document.querySelector('.welcome_text');
const navNickname = document.querySelector('.nav__nickname');
const navEmail = document.querySelector('.nav__email');
const logout = document.querySelector('.logout');
//현재날짜
const currentTime = document.querySelector('.currentTime');
//홈로고 
const logoContainer = document.querySelector('.uk-navbar-item.uk-logo');

//css hidden속성
const HIDDEN_CN = "hidden";
let isLogin = false;

window.addEventListener('DOMContentLoaded', () => {
  //navbar의 오늘날짜셋팅
  currentTime.innerText = getCurrentDate();
  checkLogin();
});

function getCurrentDate() {
  const today = new Date();
  const year = today.getFullYear();
  const month = ('0' + (today.getMonth() + 1)).slice(-2);
  const day = ('0' + today.getDate()).slice(-2);
  return `${year}년 ${month}월 ${day}일`;
}

//로고클릭시 홈화면으로 이동하고 회의나가짐
logoContainer.addEventListener('click', handlerOnLeave);

//로고화면 클릭해도 나가기 화면뜬다음 화면이동 
function handlerOnLeave() {
  location.href = `${window.location.origin}/`; 
}

//두번 호출됨
function checkLogin() {
  isLogin = JSON.parse(localStorage.getItem('isLogin'));
  
  if (isLogin) {//로그인 네비게이션바 처리
    //프로필이미지 셋팅
    profileImg.src = `${API_SERVER_URL}/api/member/profile/${localStorage.getItem('profile')}`;
    welcomeText.innerText = `${localStorage.getItem('memName')}님 환영합니다`;
    navNickname.innerText = `닉네임: ${localStorage.getItem('memName')}`;

    let email = '';
    //session의 이메일정보 요청
    const url = `${window.location.origin}/login/session`; 
    fetch(url, {
      method: 'GET',
    }).then(res => res.json())
    .then(response => {
      navEmail.innerText = response.email;;
    })
    

    beforeLogin.classList.add(HIDDEN_CN);
    profileImg.classList.remove(HIDDEN_CN);
    afterLogin.classList.remove(HIDDEN_CN);

  } else {//미로그인 네비게이션바 처리
    beforeLogin.classList.remove(HIDDEN_CN);
    profileImg.classList.add(HIDDEN_CN);
    afterLogin.classList.add(HIDDEN_CN);
  }
}
