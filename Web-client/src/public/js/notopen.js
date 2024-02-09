'use strict'

const moveTohome = document.querySelector('#moveTohome');
const moveToLogin = document.querySelector('#moveToLogin');

//홈로고 
const logoContainer = document.querySelector('.uk-navbar-item.uk-logo');


//로고클릭시 홈화면으로 이동하고 회의나가짐
logoContainer.addEventListener('click', handlerOnLeave);

//로고화면 클릭해도 나가기 화면뜬다음 화면이동 
function handlerOnLeave() {
  location.href = `${window.location.origin}/`; 
}

moveTohome.addEventListener('click', (event) => {
  location.replace(`${window.location.origin}/`);
})

moveToLogin.addEventListener('click', (event) => {
  location.replace(`${window.location.origin}/login`);
})