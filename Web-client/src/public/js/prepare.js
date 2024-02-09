'use strict';

/** 비디오스트림 */
const myFace = document.getElementById("myFace");

/** 컨트롤영역 */
const muteBtn = document.getElementById("mute");
const muteIcon = muteBtn.querySelector(".muteIcon");//음소거
const unMuteIcon = muteBtn.querySelector(".unMuteIcon");//음소서 비활성화

const cameraBtn = document.querySelector("#camera");
const cameraIcon = cameraBtn.querySelector(".cameraIcon");
const unCameraIcon = cameraBtn.querySelector(".unCameraIcon");

const enterBtn = document.querySelector('#enterBtn');
const moveHomeBtn = document.querySelector('#moveHomeBtn');

//홈로고 
const logoContainer = document.querySelector('.uk-navbar-item.uk-logo');

import { API_SERVER_URL } from './config.js'

//css hidden속성
const HIDDEN_CN = "hidden";
let muted = true;
let cameraOff = false;

let isLogin; //로그인여부
let hostId; //회의호스트ID
let meetingId; //회의ID
let isAuthSuccess; //회의참여가능 구분값 설정

let myStream;


//음소거 비활성화 아이콘 미노출
unMuteIcon.classList.add(HIDDEN_CN);

//카메라 비활성화 아이콘 미노출
unCameraIcon.classList.add(HIDDEN_CN);




//data, title, url 의 값이 들어가게 됩니다. 비워두면 이벤트 발생의 플래그 정도로 사용 할 수 있다.
//기존페이지 이외에 입력한 URL로 페이지가 하나 더 만들어진다.
history.pushState(null, null, ''); 

//뒤로가기 이벤트를 캐치해서 '회의나가기' 화면을 노출시킴
window.onpopstate = function(event) {
  // pushState로 인하여 페이지가 하나 더 생성되기 떄문에 한번에 뒤로가기 위해서 뒤로가기를 한번 더 해줍니다.
  history.back();
  location.replace(`${window.location.origin}/leave`);
};


/** 회의준비화면으로 들어왔을때 전달되는 파라미터를 처리하기 위한 코드 */
const link = window.location.href; // 사이트 주소 얻기
const url = new URL(link);

// URLSearchParams 객체를 넣는다. 그러면 메소드를 사용할수 있게 된다.
const urlParams = url.searchParams; 

// 쿼리스트링 키의 값을 가져온다.
const meetingnum = urlParams.get('meetingnum');
const meetingpwd = urlParams.get('meetingpwd');
const nickname = urlParams.get('nickname');
console.log(`meetingnum : ${meetingnum}`);
console.log(`meetingpwd : ${meetingpwd}`);
console.log(`nickname : ${nickname}`);







//회의참여 준비화면 진입시 최초 발생되는 이벤트 처리
init();

/**
 * 회의준비 회면에 진입시 최초로 체크하는 것들
 * 1. 회의코드검증
 * 2. 로그인여부
 * 3. 회의비밀번호 검증
 */
function init() {
  console.log('##### 회의준비 화면 진입...! #####');
  initMediaCall();
  

  isLogin = localStorage.getItem('isLogin');

  if (!meetingnum) {
    console.log('회의번호값이 없음...홈으로 이동!');
    location.replace(`${window.location.origin}/meeting/auth`);
    return;
  }

  //1.회의코드검증
  const codeAuthUrl = `${API_SERVER_URL}/api/meeting/verification?num=${meetingnum}`;
  fetch(codeAuthUrl, {
    method: 'GET'
  }).then(res => res.json())
  .then(response => {
    if (response.statusCode === 200) {
      console.log('1. 회의코드 검증 성공...!');

      const data = response.data;
      //hostID, meetingId값을 미리 받아놓고 회의화면 진입시 전달
      hostId = data.host_id;
      meetingId = data.meeting_id;

      //2.회의가 호스트에 의해 시작되었는지 먼저 체크
      if (data.status !== 'open') { //회의가 아직 시작하지 않은 경우
        console.log('회의가 아직 시작되지 않음...!');
        //호스트여부 체크 
        const myEmail = localStorage.getItem('email');
        if (myEmail !== hostId) {
          //호스트가 아닌경우 회의가 아직 시작안했다는 페이지로 이동
          location.replace(`${window.location.origin}/notopen`);
          return;
        }
      }



      //3. 로그인여부 확인
      if ( isLogin === 'true') {
        //회의참여가능 구분값 설정
        isAuthSuccess = true;
        return;
      } else {
        // 3. 미로그인의 경우, 회의비밀번호, 닉네임입력 유효성 체크 
        checkMeetingPwdAuth(meetingnum, meetingpwd, nickname);
      }

    } else {
      //코드가 유효하지 않다는 화면으로 이동 
      location.replace(`${window.location.origin}/meeting/auth`);
      return;
    }

  }).catch(err => {
    console.error(err);
  })

}


//회의비밀번호,  검증함수
function checkMeetingPwdAuth(meetingnum, meetingpwd, nickname) {
  if (!nickname || !meetingpwd) {
    location.replace(`${window.location.origin}/meeting/auth`);
    return;
  }

  //회의비밀번호 유효성체크
  const pwdAuthUrl = `${API_SERVER_URL}/api/meeting/verification?num=${meetingnum}&pwd=${meetingpwd}`;
  fetch(pwdAuthUrl, {
    method: 'GET'
  }).then(res => res.json())
  .then(response => {
    if (response.statusCode === 200) {//회의코드 유효성체크 성공
      isAuthSuccess = true;
      return;
    } else {
      location.replace(`${window.location.origin}/meeting/auth`);
      return;
    }
  }).catch(err => {
    console.error(err);
  })

}


//음소거 버튼 이벤트핸들러
muteBtn.addEventListener("click", handleMuteClick);
//카메라 버튼 이벤트핸들러
cameraBtn.addEventListener("click", handleCameraClick);

//로고클릭시 홈화면으로 이동하고 회의나가짐
logoContainer.addEventListener('click', handlerOnLeave);

//로고화면 클릭해도 나가기 화면뜬다음 화면이동 
function handlerOnLeave() {
  location.href = `${window.location.origin}/`; 
}


//화상통화 init함수
async function initMediaCall() { 
  await getMedia();
}


//방에 입장한 peer의 Stream정보를 가져와 비디오 및 카메라 정보를 그린다.  
async function getMedia(deviceId) {
  //기본 비디오 오디오설정
  const initialConstraints = {
    audio: true,
    video: { facingMode: "user" },
  };
  //디바이스정보가 전달될 경우 비디오설정 
  const cameraConstraints = {
    audio: true,
    video: { deviceId: { exact: deviceId } },
  };

  try {
    //전달된 deviceId값이 있을 경우, 해당 디바이스의 스트림을 노출시킨다(예: 카메라 앞/뒤 화면)
    myStream = await navigator.mediaDevices.getUserMedia(
      deviceId ? cameraConstraints : initialConstraints
    );

    // stream을 mute하는 것이 아니라 HTML video element를 mute한다.
    myFace.srcObject = myStream;

    if (!deviceId) {
      // mute default
      myStream.getAudioTracks().forEach((track) => {(track.enabled = false); });
      
      await getCameras();
    }
  } catch (error) {
    console.log(error);
  }
}


function handleMuteClick() {
  
  myStream
    .getAudioTracks()
    .forEach((track) => {
      //enabled 속성을 사용해 음소거 기능을 구현
      //오디오 track이 비활성화 되면 음소거 상태이다.
      (track.enabled = !track.enabled);
    });


  if (muted) {//음소거 상태면 미음소거 아이콘 활성화 -> 소리들림
    unMuteIcon.classList.remove(HIDDEN_CN);
    muteIcon.classList.add(HIDDEN_CN);
    muted = false;
  } else {
    muteIcon.classList.remove(HIDDEN_CN);
    unMuteIcon.classList.add(HIDDEN_CN);
    muted = true;
  }
}

function handleCameraClick() {
  myStream //
    .getVideoTracks()
    .forEach((track) => (track.enabled = !track.enabled));
  if (cameraOff) { //카메라실행되도록 아이콘 활성화
    cameraIcon.classList.remove(HIDDEN_CN);
    unCameraIcon.classList.add(HIDDEN_CN);
    cameraOff = false;
  } else {
    unCameraIcon.classList.remove(HIDDEN_CN);
    cameraIcon.classList.add(HIDDEN_CN);
    cameraOff = true;
  }
}

//비디오에 노출될 카메라정보를 가져온다.
async function getCameras() {
  try {
    const devices = await navigator.mediaDevices.enumerateDevices();
    const cameras = devices.filter((device) => device.kind === "videoinput");
    const currentCamera = myStream.getVideoTracks();
    //디바이스에 연결된 카메라 정보를 가져온다.
    cameras.forEach((camera) => {
      const option = document.createElement("option");
      option.value = camera.deviceId;
      option.innerText = camera.label;

      if (currentCamera.label == camera.label) {
        option.selected = true;
      }

      //camerasSelect.appendChild(option);
    });
  } catch (error) {
    console.log(error);
  }
}


//회의참여버튼 클릭이후 회의방 입장 
enterBtn.addEventListener('click', (e) => {
  if(isAuthSuccess) {
    console.log('회의화면 입장...!!!');
    //location.replace(`${window.location.origin}/meeting`);
    

    
    const parameter = {
      fromPrepare: true, //회의준비화면에서 진입한다는 구분값
      hostId, //회의 hostId
      meetingId //회의ID
    }
    const url = `${window.location.origin}/meeting/prepare/info`;
    
    //회의화면에 진입하면서, 필수 파라미터값(호스트ID, 진입화면구분값)을 전달
    fetch(url, {
      method: 'POST',
      mode: 'cors', //해당 옵션을 주지 않으면, body객체가 서버로 전달되지 않는다.
      headers: { //JSON Type으로 보내기위한 헤더설정
        'Accept': 'application/json',
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(parameter)
    })
    .then(res => res.json())
    .then(response => {
      //console.log('response: %o', response);
      if (response.statusCode === '200') {
        //회의화면으로 이동
        location.href = `${window.location.origin}/meeting?meetingnum=${meetingnum}&meetingpwd=${meetingpwd}&nickname=${nickname}`;
      }
    })
    .catch(err => {
      console.error(err);
    })

  }
  
})

//홈으로 이동 
moveHomeBtn.addEventListener('click', (e) => {
  //location.href = `${window.location.origin}/`;
  location.replace(`${window.location.origin}/`);
})


