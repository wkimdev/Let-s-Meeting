'use strict'

//홈으로 리디렉션
window.addEventListener('DOMContentLoaded', (e) => {
  //회의준비화면서 진입할 경우 곧바로 홈으로 이동
  setTimeout(() => {
    location.replace(`${window.location.origin}/`);
  }, 3000);
  
});
