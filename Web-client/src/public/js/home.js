'use strict'

const btnNewMeeting = document.querySelector('#btn_newMeeting');

const meetingNum = document.querySelector('#meeting_num');//회의코드(회의번호)
const startMeetiengBtn = document.querySelector('#btn_startMeetieng');

//회의참여 다이알로그
const view = document.querySelector('.DialogWrapper');
const confirm = document.getElementById('confirm');//확인버튼
const cancle = document.getElementById('cancle');//취소버튼

//회의입장모달창
const modalCenter = document.querySelector('#modal-center');
//로그인체크모달창 
const modalCheckLogin = document.querySelector('#modal-check-login');
const btnMoveToLogin = document.querySelector('.btn_moveToLogin');
const btnPopupMoveToLogin = document.querySelector('.btn_moveToLogin.notopen');
const loginCancleBtn = document.querySelector('.btn_cancle');//취소버튼


const meetingPwd = document.querySelector('#meetingPwd');
const username = document.querySelector('#username');

//회의시작확인 모달창 
const modalMeetingStatus = document.querySelector('#modal-check-meetingStatus');

//현재날짜
const currentTime = document.querySelector('.currentTime');

//네비게이션바
const beforeLogin = document.querySelector('.before__login');
const profileImg = document.querySelector('.profile_img');
const afterLogin = document.querySelector('.after__login');

//로그인시 서브 네비게이션바
const welcomeText = document.querySelector('.welcome_text');
const navNickname = document.querySelector('.nav__nickname');
const navEmail = document.querySelector('.nav__email');
const logout = document.querySelector('.logout');

//새회의생성 모달창
const modalNewMeetieng = document.querySelector('#modal-new-meeting');
const createCancleBtn = document.querySelector('.createCancleBtn');
const createMeetingBtn = document.querySelector('.createMeetingBtn');
const meetingTitle = document.querySelector('.meeting_title'); //새회의타이틀
const meetingDuration = document.querySelector('#meeting_duration'); //새회의 진행시간

const titleAlert = document.querySelector('.title__alert');
const participantNum = document.querySelector('.participant_num'); //참여자수갯수
const timePicker = document.querySelector('#timePicker'); //회의기간

import * as meeting from '../js/meeting.js'
import { API_SERVER_URL } from './config.js'

//css hidden속성
const HIDDEN_CN = "hidden";
let isLogin = false;

window.addEventListener('DOMContentLoaded', () => {
  //navbar의 오늘날짜셋팅
  currentTime.innerText = getCurrentDate();
  checkLogin();
});

// window.addEventListener('DOMContentLoaded', () => {
// })


//flatpickr timepicker 사용
//input type에 ID를 주고, 해당ID를 첫번째 인자값으로 전달해야한다.
//default 30분, 30분 간격으로 선택가능,
flatpickr('#timePicker', {
  enableTime: true,
  noCalendar: true,
  dateFormat: "H시간 i분",
  time_24hr: true,
  defaultHour: 0,
  defaultMinute: 30,
  minuteIncrement: 30
});



function getCurrentDate() {
  const today = new Date();
  const year = today.getFullYear();
  const month = ('0' + (today.getMonth() + 1)).slice(-2);
  const day = ('0' + today.getDate()).slice(-2);
  return `${year}년 ${month}월 ${day}일`;
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


//새 회의시작
btnNewMeeting.addEventListener('click', () => {

  //미로그인의 경우 로그인요청 팝업 발생
  if (!isLogin) { //미로그인
    //로그인유도 모달창
    UIkit.modal(modalCheckLogin).show();
  } else {

    //새로 생성할 회의타이틀 placeholder 지정
    meetingTitle.placeholder = `${localStorage.getItem('memName')}님의 렛츠미팅 회의`;

    //회의기간 select option생성
    const currentDate = document.querySelector('.current_date');
    currentDate.value = getCurrentDate();

    //회의생성 팝업 생성
    UIkit.modal(modalNewMeetieng).show();

  }
});

//모달창을 닫는 이벤트,
//모달이 닫힌 후 인풋박스 값들을 초기화 한다.
function closeModal(modalId){
  UIkit.modal(modalId).hide();

  if (modalId === 'createCancleBtn') {
    clearNewMeetingInput();
  }
}


//로그인화면으로 이동
btnMoveToLogin.addEventListener('click', () => {
  location.href = `${window.location.origin}/login`;
})

btnPopupMoveToLogin.addEventListener('click', () => {
  location.href = `${window.location.origin}/login`;
})

//로그인체크 모달팝업 취소 버튼 
loginCancleBtn.addEventListener('click', () => {
  //모달창 닫기
  closeModal(modalCheckLogin);
})

//새회의생성 모달팝업 취소 버튼
createCancleBtn.addEventListener('click', () => {
  closeModal(createCancleBtn);
})

let textLimit = document.querySelector('.text__length');

//회의타이틀 텍스트 길이 체크 
meetingTitle.addEventListener('input', () => {
  //100자가 넘어갔을 경우 더 입력못하도록 maxlength속성으로 이미체크
  //100자까지 길이제한체크 테스트 노출
  const textLength = meetingTitle.value.length;
  if (textLength <= 100) {
    textLimit.innerText = `${textLength}/100`;
  }
})

//회의타이틀에 입력 포커스가 갔을때만 경고문구가 뜨도록 처리
meetingTitle.addEventListener('focus', (event) => {
  titleAlert.classList.remove(HIDDEN_CN);
});
//회의타이틀에 입력 포커스가 사라졌을때 경고문구가 사라지도록 처리
meetingTitle.addEventListener('blur', (event) => {
  titleAlert.classList.add(HIDDEN_CN);
});

//회의참여인원수 셀렉트박스 선택 
participantNum.addEventListener('change', (event) => {
  
  console.log('선택한 값 : %o', participantNum.value);
})

//새회의생성 버튼 클릭
createMeetingBtn.addEventListener('click', () => {

  let title = '';
  //회의타이틀값을 가져온다. 값이 없을 경우 placeholder값을 가져온다.
  if (!meetingTitle.value) {
    title = meetingTitle.placeholder;
  } else {
    title = meetingTitle.value;
  }
  
  //회의기간값을 가져온다.
  let duration = '';
  if (timePicker.value === '00시간 00분') {
    alert('회의기간은 30분 이상으로 설정해주세요!');
    timePicker.focus();
    return;
  } else if (!timePicker.value) {
    duration = timePicker.placeholder;  
  } else {
    duration = timePicker.value;
  }

  console.log('회의기간: %o', duration);

  //회의참여자수
  const participantNumber = participantNum.value;

  console.log('회의참여자수 : %o', participantNumber);

  //DB에 새회의생성 요청 
  //DB에서 응답받은 회의정보로, 회의화면 진입

  //1) 회의진입 로딩바 뜬다음 --> 연결중, 로딩중 텍스트 노출
  //2) 회의화면 뜸

  //'2022-02-09 12:30:00'  

  const email = localStorage.getItem('email');
  const startDate = moment().format("YYYY-MM-DD");
  const startTime = moment().format("HH:mm:ss");

  console.log(`hostID : ${email}, participantNum: ${parseInt(participantNumber)}`);

  //
  const parameter = {
    title,
    startDate, 
    startTime, 
    "gmt": "KO",
    duration,
    email,
    'hostId': email,
    'participantNum': parseInt(participantNumber)
  }
  console.log('paramter: %o', parameter);

  
  /**
   * API서버에 새회의생성 요청
   * -> 한 도메인에서 다른도메인으로 요청시
   * 요청설정에 CORS설정이 되어 있어야 하고
   * 수신도메인은(API서버) 요청 도메인을 허용시켜놓아야 한다.
   */
  const url = `${API_SERVER_URL}/api/meeting/new`;
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

    if (response.statusCode === 200) {
      
      meeting.getMeetingInfo(response.meetingId)
        .then(data => {
          console.log('회의번호 : %o', data.meeting_num);    

          //회의화면으로 이동 - 로그인상태이기 때문에 회의ID만 파라미터로 넘긴다.
          location.href = `${window.location.origin}/meeting?meetingnum=${data.meeting_num}&meetingpwd=&nickname=`;

          //새회의생성 모달창을 닫고 input box에 있는것 초기화 한다.
          closeModal(createCancleBtn);
        });

    } else {
      alert('오류 발생! :' + data.message);
      location.href = `${window.location.origin}/`;
      closeModal(createCancleBtn);
      return;
    }

  }).catch(error => {
    alert('일시적으로 오류가 발생했습니다.');
    console.error(error);
    closeModal(createCancleBtn);
    return;
  })

})

function clearNewMeetingInput() {
  //새회의생성 인풋박스 초기화
  meetingTitle.value = '';
  timePicker.value = '00시간 30분';
  participantNum.options[1].selected = true;
}



/**
 * 회의코드 입력후 '시작' 버튼 클릭
 */
startMeetiengBtn.addEventListener('click', () => {

  if (!meetingNum.value) {
      meetingNum.focus();
      alert("회의코드를 입력해주세요!");
      return;
  }

  //1. 회의코드 유효성 체크
  const url = `${API_SERVER_URL}/api/meeting/verification?num=${meetingNum.value}&pwd=`
  fetch(url, {
    method: 'GET'
  })
  .then(res => res.json())
  .then(response => {
    if (response.statusCode === 200) {//회의코드 유효성체크 성공


      //2.회의가 호스트에 의해 시작이 되었는지 먼저 체크
      const data = response.data;
      debugger
      if (data.status !== 'open') { //회의가 아직 시작하지 않은 경우

        //호스트여부 체크 
        const myEmail = localStorage.getItem('email');
        if (myEmail !== data.host_id) {
          //호스트가 아닌경우 회의가 아직 시작안했다는 팝업발생
          UIkit.modal(modalMeetingStatus).show();
          
          //회의번호 초기화
          meetingNum.value = '';
          return;
        }
      }

      //3. 로그인여부 확인
      if (isLogin) {

        //곧바로 회의준비 화면 이동
        moveToPrepareScreen(meetingNum.value, '', localStorage.getItem('memName'));
        return;
      } else {
        // 4. 미로그인의 경우, 회의비밀번호, 닉네임입력 팝업발생 
        UIkit.modal(modalCenter).show();
      }

    } else {
      alert('회의ID가 유효하지 않습니다. 확인하고 다시 시도해주세요.');
      //회의번호 초기화
      meetingNum.value = '';
      meetingNum.focus();
      return;
    }

  }).catch(err => {
    console.error(err);
  })



  //취소버튼 클릭 후 모달창 닫기
  //클릭 이벤트가 두번들어오는 오류 발생중
  cancle.addEventListener("click", (event) => {
      //초기화
      meetingPwd.value = '';
      username.value = '';
      meetingNum.value = '';
  })


  //회의정보 검증 후 회의방입장
  confirm.addEventListener("click", (event) => {
    event.preventDefault();

    if (!meetingPwd.value) {
      meetingPwd.focus();
      alert("회의비밀번호를 입력해주세요!");
      return;
    }

    if (!username.value) {
      username.focus();
      alert("유저명을 입력해주세요!");
      return;
    }

    //회의비밀번호 유효성체크
    const pwdAuthUrl = `${API_SERVER_URL}/api/meeting/verification?num=${meetingNum.value}&pwd=${meetingPwd.value}`;
    fetch(pwdAuthUrl, {
      method: 'GET'
    }).then(res => res.json())
    .then(response => {
      if (response.statusCode === 200) {//회의코드 유효성체크 성공

        //회의비밀번호와 닉네임포함하여 회의준비화면 이동
        moveToPrepareScreen(meetingNum.value, meetingPwd.value, username.value);

        //초기화
        meetingPwd.value = '';
        username.value = '';
        meetingNum.value = '';

      } else {
        meetingPwd.focus();
        alert('회의비밀번호가 일치 하지 않습니다. 다시 확인해주세요!');
        return;
      }

      //모달창을 닫늗다
      UIkit.modal(modalCenter).hide();

    }).catch(err => {
      console.error(err);
    })

  })

})


//회의준비화면으로 이동하는 함수
function moveToPrepareScreen(meetingNum, meetingpwd = '', nickname = '') {
  //페이지 이동시 파라미터를 넘기면서 이동
  location.href = `${window.location.origin}/prepare?meetingnum=${meetingNum}&meetingpwd=${meetingpwd}&nickname=${nickname}`;
}


//로그아웃을 시키면서 
//로컬스토리지 로그인상태값을 false로 변경후, 프로필사진, 닉네임을 삭제한다. 
logout.addEventListener('click', () => {
  
  //아이디저장 체크가 되어있으면 이메일은 지우지 않고 유지한다
  if (localStorage.getItem('isSaveUserId') !== 'true') {
    localStorage['email'] = '';
  }
  localStorage['isLogin'] = false;
  localStorage['profile'] = '';
  localStorage['memName'] = '';

  //refresh
  //서버에 로그아웃 요청
  location.href = `${window.location.origin}/logout`;
})