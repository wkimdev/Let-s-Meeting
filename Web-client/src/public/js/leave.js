'use strict'

//홈으로 리디렉션
window.addEventListener('DOMContentLoaded', (e) => {
  console.log('이전url :' + document.referrer);

  //http://localhost:3004/meeting
  // let previousUrl = document.referrer;
  // const splitUrl = previousUrl.split('/');

  //회의준비화면서 진입할 경우 곧바로 홈으로 이동
  setTimeout(() => {
    //location.href = `${window.location.origin}/`; 
    location.replace(`${window.location.origin}/`);
  }, 1500);
  
});

//회의준비화면으로 리디렉션하려면 ?
