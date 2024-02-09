
const moveHomeBtn = document.querySelector('#moveHomeBtn');

//홈으로 이동 
moveHomeBtn.addEventListener('click', (e) => {
  //location.href = `${window.location.origin}/`;
  location.replace(`${window.location.origin}/`);
})
