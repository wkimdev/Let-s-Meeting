/** 소켓클라이언트 */
const socket = io();

/** 비디오스트림 */
const meetingMain = document.querySelector('.meetingMain'); //화상회의 메인돔
const myFace = document.getElementById("myFace"); //내화면을 그리는 비디오태그 돔

/** 컨트롤영역 */
const muteBtn = document.getElementById("mute"); //음소거버튼 영역
const muteIcon = muteBtn.querySelector(".muteIcon");//음소거
const unMuteIcon = muteBtn.querySelector(".unMuteIcon");//음소서 비활성화

const cameraBtn = document.querySelector("#camera"); //카메라버튼 영역
const cameraIcon = cameraBtn.querySelector(".cameraIcon"); //카메라노출 아이콘
const unCameraIcon = cameraBtn.querySelector(".unCameraIcon"); //카메라미노출 아이콘
const screenShareIcon = document.querySelector(".screenShare"); //화면공유 아이콘
const unScreenShareIcon = document.querySelector(".unScreenShare"); //화면공유 비활성화 아이콘
const camerasSelect = document.getElementById("cameras"); //카메라모니터 선택영역
const leaveBtn = document.getElementById('leave'); //회의나가기 버튼

/** 회의정보확인 */
const infoIcon = document.getElementById('info__icon'); //회의정보 아이콘
const modalInfo = document.querySelector('.modal__info'); //회의정보 모달창
const modalInfoBox = document.querySelector('.modal__info-box'); //회의정보 모달창

/** 화면공유 */
const screenShareView = document.querySelector('.screen__share'); //화면공유 영역
screenShareView.style.display = 'none'; //화면공유전까진 미노출 처리

import * as meeting from './meeting.js'  //회의정보관련 API
import { API_SERVER_URL } from './config.js' //서버API 설정


/** 파일전송 프로그레스바 */
let progressbar;
let progressLabel;
let fileDown;

//import test from './fileshare.js';

//css hidden속성
const HIDDEN_CN = "hidden";

//파일전송시 청크최대 사이즈64KB
const MAXIMUM_MESSAGE_SIZE = 65535;
const END_OF_FILE_MESSAGE = 'EOF';


let roomName = "";
let nickname = "";
let roomMeetingId = ""; //회의삭제시 사용하는 회의ID
let peopleInRoom = 1; //회의방에 있는 참가자수 초기화
let myStream;
let muted = true;
let cameraOff = false;
let myPeerConnection;

let file; //공유할 파일객체
let isLogin = false; //로그인여부
let profile = null; //프로필사진

//파일전송 datachannel 처리를 위한 변수선언
let receiveBuffer = [];
let receivedSize = 0;
let receivedFile = {}

//화면공유시 미디어스트림정보를 담을 객체
let displayMediaStream;
//내 socketId
let mySocketId;
//화면공유자인지 구분하는 값
let isScreenShare = false;
let isMyScreenShare = false;

//회의정보팝업 내용UI를 담는 변수
let popupUI = '';


//음소거 비활성화 아이콘 미노출
unMuteIcon.classList.add(HIDDEN_CN);

//카메라 비활성화 아이콘 미노출
unCameraIcon.classList.add(HIDDEN_CN);

//화면공유 비활성화 아이콘 미노출
unScreenShareIcon.classList.add(HIDDEN_CN);


//3명이상의 참여자가 발생할 경우,  
//각 유저별 SocketId를 키값으로 RTCPeerConnection객체를 생성해서 관리하기 위한 객체선언
let pcObj = {
  // remoteSocketId: pc
};

//3명이상의 참여자가 발생할 경우,  
//각 유저별 SocketId를 키값으로 RTCDatachannel객체를 생성해서 관리하기 위한 객체선언
//파일공유 데이터채널 객체
let chObj = {};
//채팅데이터채널 객체
let chatChObj = {}; 
let maximum = 0; //회의최대 참여인원수
let myId = ''; //로그인했을 경우 사용자 이메일정보

//data, title, url 의 값이 들어가게 됩니다. 비워두면 이벤트 발생의 플래그 정도로 사용 할 수 있다.
//기존페이지 이외에 입력한 URL로 페이지가 하나 더 만들어진다.
history.pushState(null, null, ''); 

//뒤로가기 이벤트를 캐치해서 회의준비화면으로 이동되도록 처리함
window.onpopstate = function(event) {
  // pushState로 인하여 페이지가 하나 더 생성되기 떄문에 한번에 뒤로가기 위해서 뒤로가기를 한번 더 해줍니다.
  history.back();
  location.replace(`${window.location.origin}/leave`);
};


/** 회의화면으로 들어왔을때 전달되는 파라미터를 처리하기 위한 코드 */
const link = window.location.href; // 사이트 주소 얻기
const url = new URL(link);

// URLSearchParams 객체를 넣는다. 그러면 메소드를 사용할수 있게 된다.
const urlParams = url.searchParams;

// 쿼리스트링 키의 값을 가져온다.
const meetingnum = urlParams.get('meetingnum'); //회의번호 
const meetingpwd = urlParams.get('meetingpwd'); //회의비밀번호
const myNickname = urlParams.get('nickname'); //유저명



//테스트코드 때문에 사용하는 코드
window.addEventListener('DOMContentLoaded', (e) => {
});

//회의정보노출 팝업 클릭이벤트처리
infoIcon.addEventListener("click", () => {
  if (modalInfo.className.includes('hidden')) { //회의정보 모달팝업창이 hidden상태면 노출시킨다.
    modalInfo.classList.remove(HIDDEN_CN);
  } else {  //회의정보 모달팝업창이 hidden상태가 아니면 노출 시키지 않는다.
    modalInfo.classList.add(HIDDEN_CN);
  }
});


//회의참여화면 진입시 최초 발생되는 이벤트 처리
callInitPeer();

async function callInitPeer(){

  console.log('##### 회의참여화면 진입...! #####');

  if (socket.disconnected) {
    socket.connect();
  }

  //session에 로그인된 이메일값 요청
  getLoginValue().then(result => {
    myId = result.email;
  });
  
  //호스트ID값과 회의준비화면으로부터 진입했는지 확인요청하는 이벤트
  socket.emit('request_meeting_info');

  //미디어 스트림 정보요청
  await initCall();

  //로컬스토리지에서 로그인여부 및 프로필사진 호출
  isLogin = localStorage.getItem('isLogin');
  profile = localStorage.getItem('profile');
}

//로그인된 이메일 정보 요청
function getLoginValue() {
  //session의 이메일정보 요청
  const url = `${window.location.origin}/login/session`;
  return fetch(url, {
    method: 'GET'
  })
  .then(response => {
      if (!response.ok) {
          return response.json()
              .catch(() => {
                  // Couldn't parse the JSON
                  throw new Error(response.status);
              })
              .then(({message}) => {
                  // Got valid JSON with error response, use it
                  throw new Error(message || response.status);
              });
      }
      return response.json();
  })
  .then(result => {
    return result;
  });
}


/** 
 * 서버로부터 현재 회의에 입장한 사용자 정보와 기타 회의관련 정보를 전달 받고 
 * 회의준비화면으로 부터 진입했는지 아닌지(fromPrepare)값에 따라 처리 로직이 달라짐.
 * hostId - 회의호스트아이디
 * fromPrepare - 회의준비화면으로 부터 진입했는지 구분값
 * meetingId - 회의아이디   
 * localSocketId - 내 소켓ID
 * screenShareStatus - 화면공유상태 구분값
*/
socket.on('send_meeting_info', (hostId, fromPrepare, meetingId, localSocketId, screenShareStatus) => {
  //console.log('#### send_meeting_info 호출....!');
  roomMeetingId = meetingId;
  mySocketId = localSocketId;
  isScreenShare = screenShareStatus;

  //회의준비화면에서 진입했다면, 이미 회의정보에 대한 유효성검증이 끝났기 때문에 
  //곧바로 회의참석이 되도록 진행한다.
  if (fromPrepare) {

    //회의상태값 확인 
    meeting.getMeetingStatus(meetingnum).then(data => {
      
      console.log('회의상태값 : %o', data.status);

      //회의최대참여 인원수 설정 
      maximum = data.participants_num;

      //const myId = localStorage.getItem('email');

      //회의시작 여부 확인
      if (data.status === 'close') {
        //호스트여부 확인 후 회의시작상태로 변경
        if (myId === hostId) {
          meeting.openMeetingStatus(myId, roomMeetingId);
          console.log('회의상태값 open으로 변경!..');
        } else {
          //호스트가 아닌경우 회의가 아직 시작안했다는 페이지로 이동
          location.replace(`${window.location.origin}/notopen`);
          return;
        }
      }
      //회의정보 팝업UI생성 요청
      popupUI = meeting.createMeetingInfoPopup(data);
      modalInfoBox.innerHTML = popupUI;

      writeChat(`[${myNickname}] 님이 회의에 참여했습니다.`, NOTICE_CN);
    
      //회의화면에 유저닉네임 셋팅
      const nicknameValue = document.querySelector("#userNickname");
      //회의URL 쿼리파라미터로부터 받은 회의정보를 전역변수에 바인딩
      console.log(`>>>>>>> myId : ${myId}, hostId: ${hostId}`);
      if (myId === hostId) {
        nickname = `${myNickname} (호스트)`;
      } else {
        nickname = myNickname;
      }
      //회의에 진입한 사람이 '나'인지 표기함
      nicknameValue.innerText = `${nickname}[나]`;
      roomName = meetingnum;
  
      //회의방에 입장했다는 이벤트 emit 후, 'welcome'이벤트를 받을 준비를 하게 된다.
      console.log('join_room emit...!!! maximum : %o', maximum);
      socket.emit('join_room', roomName, nickname, maximum);
    });
  } else {

    isLogin = localStorage.getItem('isLogin');

    /**
     * 회의준비화면에서 진입한 경우가 아닐경우
     * 1.회의코드 2.로그인여부 3.회의비밀번호 검증 순서대로 유효성을 검증한다. 
      검증한 다음 회의준비화면으로 redirect 혹은 검증실패 화면으로 이동한다.
     */

    //1.meetingnum값이 없을 경우 먼저 reutrn처리
    if (!meetingnum) {
      console.log('회의번호값이 없음...홈으로 이동!');
      location.replace(`${window.location.origin}/meeting/auth`);
      return;
    }

    //2. 회의코드 유효성체크
    meeting.authMeeting(meetingnum).then(result => {
      console.log('코드검증 결과값 확인: %o', result);
      
      //회의최대참여 인원수 설정 
      maximum = result.participants_num;

      if (!result) {
        console.log('회의코드값이 유효히지 않음...홈으로 이동!');
        location.replace(`${window.location.origin}/meeting/auth`);
        return;
      }

      //3.로그인여부체크
      if (isLogin === 'true') {
        console.log('회의코드값이 유효하고 로그인함...회의준비화면으로 이동!');
        meeting.moveToPrepareScreen(meetingnum, '', localStorage.getItem('memName'));
        return;
      } 

      //4.미로그인의 경우 회의비밀번호, 닉네임이 있는지 여부
      if (!myNickname || !meetingpwd) {
        console.log('닉네임 또는 비밀번호가 없음...홈으로 이동!');
        location.replace(`${window.location.origin}/meeting/auth`);
        return;
      }

      //5.회의비밀번호 검증
      meeting.authMeeting(meetingnum, meetingpwd)
        .then(result => {
          console.log('패스워드 검증결과 확인..! : %o', result);
          if (!result) {
            console.log('회의코드값이 유효히지 않음...홈으로 이동!');
            location.replace(`${window.location.origin}/meeting/auth`);
            return;
          }

          //회의정보 팝업UI생성 요청
          popupUI = meeting.createMeetingInfoPopup(result);
          modalInfoBox.innerHTML = popupUI;

          console.log('미로그인상태 회의코드값이 유효하고...회의준비화면으로 이동!');
          meeting.moveToPrepareScreen(meetingnum, meetingpwd, myNickname);
        })
    })
  }
})



//음소거 버튼 이벤트핸들러
muteBtn.addEventListener("click", handleMuteClick);
//카메라 버튼 이벤트핸들러
cameraBtn.addEventListener("click", handleCameraClick);
//카메라선택 이벤트핸들러
camerasSelect.addEventListener("input", handleCameraChange);

function handleMuteClick() {
  console.log('handleMuteClick...! myStream : %o', myStream);
  
  //test로 주석처리
  myStream
    .getAudioTracks()
    .forEach((track) => {
      //enabled 속성을 사용해 음소거 기능을 구현
      //오디오 track이 비활성화 되면 음소거 상태이다.
      (track.enabled = !track.enabled);
      console.log('track.enabled값 확인 :' + track.enabled);
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

/** 
 * 비디오가 변경되었을때 sender라는 것을 통해 track을 업데이트 하는 코드
 * 캠이 하나밖에 없어서 테스트는 못 함 
 * peerConnectionObjArr을 어디서 설정하는지 몰라 주석처리함(기능은 정상동작함)
*/
async function handleCameraChange() {
  try {
    //방에 입장한 peer의 Stream정보를 가져온다
    //카메라가 변경될때마다 constraint을 전달받도록 처리
    //사용할 카메라 정보를 던진다.
    await getMedia(camerasSelect.value);

    /** 
    if (peerConnectionObjArr.length > 0) {
      //stream의 비디오트랙을 나타내는 MediaStreamTrack 객체의 시퀀스를 반환합니다
      const newVideoTrack = myStream.getVideoTracks()[0];

      peerConnectionObjArr.forEach((peerConnectionObj) => {
        //RTCPeerConnection 객체
        const peerConnection = peerConnectionObj.connection;
        //배열의 각 객체는 하나의 트랙의 데이터의 송신을 담당하는 RTP sender를 나타낸다.
        // Sender 객체는 트랙 데이터의 인코딩과 송신을 확인하고, 조작 할 수 있는 메소드와 속성들을 제공
        const peerVideoSender = peerConnection
          .getSenders()
          .find((sender) => sender.track.kind == "video");

          //카메라를 전환해줌
          peerVideoSender.replaceTrack(newVideoTrack);
      });
    }*/

  } catch (error) {
    console.log(error);
  }
}


// Leave Room

function leaveRoom() {

  //회의호스트와 구분 
  if (nickname.includes('호스트')) {
    //모달팝업
    paintModal('모든참여자를 내보내고 회의를 종료하시겠습니까?');
  } else {
    //모달팝업
    paintModal('정말 회의에서 나가시겠어요?');
  }
}

//socket커넥션을 끊고, 회의방을 나가면서 전역 변수들을 초기화하는 함수
function doLeaveRoom() {
  //수동으로 socket 연결을 끊는다.
  socket.disconnect();

  meetingMain.classList.add(HIDDEN_CN); //회의화면 미노출

  pcObj = [];
  chObj = [];
  peopleInRoom = 1;
  nickname = "";

  myStream.getTracks().forEach((track) => track.stop());
  const nicknameContainer = document.querySelector("#userNickname");
  nicknameContainer.innerText = "";

  myFace.srcObject = null;
  //비디오 스트림 화면 삭제
  clearAllVideos();

  //채팅내용 클리어
  clearAllChat();

  //화면공유스크린 초기화
  removeScreenShareUI();
}


//socket서버의 leave_room 이벤트를 받고 상대방 회의방 나가기 처리
socket.on("leave_room", (leavedSocketId, nickname, isScreenShare, isSharerLeave) => {
  console.log('leave_room listen...!!!!');

  //화면공유자일 경우 상대방측에서 공유되는 화면이 사라져야 한다.
  console.log("############# 2. 화면공유값확인 isScreenShare : %o", isScreenShare);
  
  //상대방이 나갈 때마다, 해당 이벤트가 호출되기 때문에 화면을 유지하거나 삭제하는 처리 필요

  //회의를 나갔을 때 화면공유UI 처리
  //  내가 화면을 공유하는 상태이고, 상대방이 방을 나가면 -> 화면유지
  //  내가 화면을 공유하는 상태이고, 내가 방을 나가면 -> 상대방 화면에서 삭제
  if (peopleInRoom == 2) {
    if (isSharerLeave && !isMyScreenShare) {
      document.getElementById('screen__view').srcObject = null;
      removeScreenShareUI();
    }
  }

  //  3명 이상일때, 상대방이 화면을 공유중이고 그 외 사용자가 회의를 나갓을 경우 -> 화면유지
  //  내가 화면을 공유하는 상태이고, 내가 방을 나가면 -> 상대방 화면에서 삭제
  if (peopleInRoom >= 3) {
    //화면공유자가 나가면 화면을 지운다. 
    if (isSharerLeave) {
      document.getElementById('screen__view').srcObject = null;
      removeScreenShareUI();
    }
  }
  
  //떠난 참가자의 비디오 삭제
  removeVideo(leavedSocketId);

  //아예 진입을 못한경우 작성되면 안됨
  if (nickname) {
    writeChat(`[${nickname}] 님이 회의를 나갔습니다.`, NOTICE_CN);
  }
  
  --peopleInRoom;
  //새로고침이나 뒤로가기로 회의화면진입시, 소켓커넥션이 끊기면서 참여자수를0으로 만드는 경우가 있어, 
  //해당 경우에만 회의참여자수를 다시 카운트함
  if (peopleInRoom === 0) {
    checkPeopleInRoom();
  }
  sortStreams(isScreenShare);
});

//새로고침이나 뒤로가기로 회의화면진입시, 소켓커넥션이 끊기면서 참여자수를0으로 만드는 경우가 있어, 
//해당 경우에만 회의참여자수를 다시 카운트함
function checkPeopleInRoom() {
  const videoDom = document.querySelectorAll('video');
  videoDom.forEach(video => { 
    if(video.id !== 'screen__view') { 
      peopleInRoom++;
    }
  })
}

//호스트에 의해 회의가 종료되었다는 이벤트를 받음
socket.on('noti_finish_meeting', () => {
  console.log('호스트에 의해 회의가 종료되엇습니다....!!!! ');
  //팝업이 뜬뒤 회의종료화면으로 이동
  paintModal('호스트에 의해 회의가 종료되었습니다. 회의를 나갑니다.', true);
})

//떠난 참가자의 비디오 삭제
function removeVideo(leavedSocketId) {
  const streams = document.querySelector(".screen");
  const streamArr = streams.querySelectorAll("div");
  streamArr.forEach((streamElement) => {
    //나가는 참가자의 소켓ID의 경우 해당 DOM삭제
    if (streamElement.id === leavedSocketId) {
      streams.removeChild(streamElement);
    }
  });
}


function clearAllVideos() {
  const streams = document.querySelector(".screen");
  const streamArr = streams.querySelectorAll("div");
  streamArr.forEach((streamElement) => {
    if (streamElement.id != "myStream") {
      streams.removeChild(streamElement);
    }
  });
}

leaveBtn.addEventListener("click", leaveRoom);

// Modal code

const modal = document.querySelector(".modal");
const modalText = modal.querySelector(".modal__text");

//회의나가기 버튼 영역
const modalBtnBox = modal.querySelector('.modal__btn-box');
const modalBtn = modal.querySelector(".modal__btn");
const modalCancleBtn = modal.querySelector(".cancle__btn");
//화면공유방지 팝업
const modal_stopScreenShare = document.querySelector(".modal.stopScreenShare");
const okBtn = modal_stopScreenShare.querySelector(".modal__btn");

//호스트에 의해 회의종료시 노출되는 버튼 영역 
const modalAllLeave = modal.querySelector('.modal__all_leave');


//화면공유방지 팝업
function paintScreenShareModal() {
  modal_stopScreenShare.classList.remove(HIDDEN_CN);
}
//화면공유방지 팝업에서 OK버튼 클릭했을 경우
okBtn.addEventListener('click', () => {
  modal_stopScreenShare.classList.add(HIDDEN_CN);
})

function paintModal(text, finishByHost = false) {
  modalText.innerText = text;
  modal.classList.remove(HIDDEN_CN);

  if (finishByHost) {
    modalAllLeave.classList.remove(HIDDEN_CN)
    modalBtnBox.classList.add(HIDDEN_CN);

    modalAllLeave.addEventListener('click', leaveMeetingRoom)

  } else {
    modal.addEventListener("click", removeModal);
    modalCancleBtn.addEventListener("click", removeModal);
    modalBtn.addEventListener("click", removeModalAndLeave);
  }

  //document.addEventListener("keydown", handleKeydown);
}

//호스트에 의해 회의가 종료된 이후 회의 종료화면으로 이동한다.
function leaveMeetingRoom () {

  //회의종료 화면으로 이동해야 한다.
  location.replace(`${window.location.origin}/finish`);

  //socket종료 및 데이터 초기화
  doLeaveRoom();
}

function removeModal() {
  modal.classList.add(HIDDEN_CN);
  modalText.innerText = "";
}


//삭제버튼 클릭후 모달삭제
function removeModalAndLeave() {
  console.log('회의종료 버튼 클릭!');
  modal.classList.add(HIDDEN_CN);
  modalText.innerText = "";

  /**
   * 회의호스트의 경우 아래의 동작실행
   * 1) API서버로 회의삭제 요청 
   * 2) 회의에 참여중인 다른유저에게 종료 Noti
   * 3) 소켓 커넥션종료
   */
  if (nickname.includes('호스트')) {
    console.log('####### 호스트가 회의를 종료함!!!');
    //회의삭제요청
    meeting.deleteMeeting(roomMeetingId)
        .then(result => {
          if(result) { //회의삭제성공
            console.log('####### 호스트가 회의를 종료했다는 노티를 함!!!');
            socket.emit('finish_meeting_by_host');
            //socket종료 및 데이터 초기화
            doLeaveRoom();
            //회의종료 화면으로 이동해야 한다.
            location.replace(`${window.location.origin}/finish`);
            return;
          }
        })
    
  } else  {//호스트가 아닌경우 회의만 나가도록 처리

    //회의종료 화면으로 이동해야 한다.
    location.replace(`${window.location.origin}/finish`);

    //socket종료 및 데이터 초기화
    doLeaveRoom();
  }
}

//Esc key, Enter클릭 후 모달 삭제
// function handleKeydown(event) {
//   if (event.code === "Escape" || event.code === "Enter") {
//     removeModal();
//     //팝업이 뜨고, 이전화면으로 넘어가야 한다.
//     meetingMain.classList.add(HIDDEN_CN);
//   }
// }

//화상통화 init함수
async function initCall() { 
  await getMedia();
}

//방에 입장한 peer의 Stream정보를 가져와 비디오 및 카메라 정보를 그린다.  
async function getMedia(deviceId) {
  console.log('1. getMedia call...!');

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
    console.log('2. myStream 정보 : %o', myStream);

    // stream을 mute하는 것이 아니라 HTML video element를 mute한다.
    myFace.srcObject = myStream;

    if (!deviceId) {
      console.log('3. getMedia call...!');
      // mute default
      myStream.getAudioTracks().forEach((track) => {(track.enabled = false); });
      console.log('4. getMedia call myStream 값 확인 : %o ', myStream);

      await getCameras();
    }
  } catch (error) {
    console.log(error);
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
      camerasSelect.appendChild(option);
    });
  } catch (error) {
    console.log(error);
  }
}

//발표자 
const speaker = document.querySelector('.speaker');
//화면공유 취소 버튼 
const btnStopShare = document.querySelector('.stop_share');

//화면공유 아이콘 클릭
screenShareIcon.addEventListener('click', async (e) => {
  let screenView = document.getElementById('screen__view').srcObject;
  //이미 다른사람이 화면을 공유중이면 공유할 수 없다는 팝업을 띄움
  if (screenView) {
    paintScreenShareModal();
    return;
  }

  //공유한 화면값이 없을 경우만 브라우저에 스크린캡쳐창이 뜬다
  if (!displayMediaStream) {
    //사용자 화면에 액세스하는 기능
    //콘텐츠가 사용자가 선택한 화면 영역에서 가져온 비디오 트랙 또는 오디오 트랙을 포함하는 것을 리턴.
    displayMediaStream = await navigator.mediaDevices.getDisplayMedia();
    
    //화면공유상태 체크
    isScreenShare = true;
    //내가 화면을 공유한 상태인지 체크
    isMyScreenShare = true;
  }
  console.log('########## 1. 화면공유자 구분값 확인: %o', isScreenShare);

  const count = Object.keys(pcObj).length;
  if (count >= 1) {
    //연결된 피어객체들을 가져온다.
    const PCObject = Object.values(pcObj);
    const PCObjectKey = Object.keys(pcObj);

    //새 미디어트랙으로 교체하기 위해 Peer객체별로 미디어트랙을 전환하도록 실행
    PCObject.forEach(pcObj => {
      //getSenders(): RTCRtpSender객체의 배열을 반환
      //배열의 각 객체는 하나의 트랙의 데이터의 송신을 담당하는 RTP sender를 나타냅니다.
      //Sender 객체는 트랙 데이터의 인코딩과 송신을 확인하고, 조작 할 수 있는 메소드와 속성.
      const peerVideoSender = pcObj.getSenders()
                              .find((sender) => sender.track.kind == "video");
      //새 미디어트랙으로 전환해준다.
      peerVideoSender.replaceTrack(displayMediaStream.getTracks()[0]);
    })

    //다른 피어에 화면공유가 시작되었다는 이벤트를 보낸다. 
    PCObjectKey.forEach(remoteSocketId => {
      socket.emit('screen_share', remoteSocketId, mySocketId, myNickname);
    })
  }

  //화면공유UI 노출
  screenShareView.style.display = 'flex';
  //로컬화면에서 공유된 화면으로 변경
  document.getElementById('screen__view').srcObject = displayMediaStream;
  document.getElementById('myFace').srcObject = displayMediaStream;

  //화면공유 UI로 변경
  addScreenShareUI(myNickname);
  //발표자의 경우만 화면공유 취소버튼 노출
  btnStopShare.style.display = 'block';
  
  //화면공유 아이콘 미노출, 화면공유 취소 아이콘 노출
  screenShareIcon.style.display = 'none';
  unScreenShareIcon.style.display = 'block';
})

//화면공유 UI로 변경하는 함수
function addScreenShareUI(nickname) {
  console.log('nickanme확인 : %o', nickname);
  const allVideoTag = document.querySelectorAll('video');

  //발표중인 사용자 닉네임 셋팅
  speaker.innerText = `${nickname}발표중`;
  
  //화면공유상태일 경우, 비디오 크기 재설정
  allVideoTag.forEach(item => {
    if (item.id !== 'screen__view') {
      item.width = 200;
      item.height = 180;
    }
  })
  sortStreams(isScreenShare = true);
}

//화면공유 UI제외
function removeScreenShareUI(){
  console.log('###### 화면공유스크린 미노출 함수 호출!!!!!!!!');
  //화면공유스크린 미노출
  screenShareView.style.display = 'none';

  //유저화면 원래CSS로 적용되도록 처리
  const streams = document.querySelector(".screen");
  streams.classList.remove('top');

  //발표자 초기화
  speaker.innerText = ``;

  const allVideoTag = document.querySelectorAll('video');
  allVideoTag.forEach(item => {
    if (item.id !== 'screen__view') {
      item.width = 400;
    }
  })
  sortStreams(isScreenShare = false);
}

//화면공유취소 버튼 클릭 이벤트처리
btnStopShare.addEventListener('click', () => {

  isScreenShare = false; //화면공유구분값 초기화
  isMyScreenShare = false; //내가 화면공유를 취소함
  displayMediaStream = ''; //스크린공유 미디어객체값 초기화
  stopCapture();  //화면공유 중지 함수 호출

  //화면공유 아이콘 노출, 화면공유 취소 아이콘 미노출
  screenShareIcon.style.display = 'block';
  unScreenShareIcon.style.display = 'none';
  //화면공유 취소버튼 미노출
  btnStopShare.style.display = 'none';
})

//화면공유취소 아이콘 클릭 이벤트처리
unScreenShareIcon.addEventListener('click', () => {
  
  isScreenShare = false; //화면공유구분값 초기화
  isMyScreenShare = false; //내가 화면공유를 취소함
  displayMediaStream = ''; //스크린공유 미디어객체값 초기화
  stopCapture();  //화면공유 중지 함수 호출

  //화면공유 아이콘 노출, 화면공유 취소 아이콘 미노출
  screenShareIcon.style.display = 'block';
  unScreenShareIcon.style.display = 'none';
  //화면공유 취소버튼 미노출
  btnStopShare.style.display = 'none';
})

//화면공유 중지 함수
function stopCapture() {
  const myVideo = document.getElementById('myFace').srcObject;
  
  //내 로컬비디오에서 트랙값을 가져와 현재 미디어트랙을 중지한다.
  let tracks = myVideo.getTracks();
  tracks.forEach(track => track.stop());

  //변경된 새 비디오트랙을 가져온다.
  const newVideoTrack = myStream.getVideoTracks()[0];

  //1) 다른피어에서도 화면 변경된 상태로 돌리고
  //2) 화면공유 UI 없애면 됨
  const count = Object.keys(pcObj).length;
  if (count >= 1) {
    //연결된 피어객체들을 가져온다.
    const PCObject = Object.values(pcObj);
    const PCObjectKey = Object.keys(pcObj);
    
    PCObject.forEach(pcObj => {
      //getSenders(): RTCRtpSender객체의 배열을 반환
      //배열의 각 객체는 하나의 트랙의 데이터의 송신을 담당하는 RTP sender를 나타냅니다.
      //Sender 객체는 트랙 데이터의 인코딩과 송신을 확인하고, 조작 할 수 있는 메소드와 속성.
      const peerVideoSender = pcObj.getSenders()
                              .find((sender) => sender.track.kind == "video");
      //새 미디어트랙으로 전환해준다.
      peerVideoSender.replaceTrack(newVideoTrack);
    })
  
    //다른 피어에 업데이트 이벤트를 보내야 한다. 
    PCObjectKey.forEach(remoteSocketId => {
      socket.emit('screen_share_stop', remoteSocketId, mySocketId);
    })
  }

  //로컬 화면을 원래대로 되돌려놓는다.
  document.getElementById('screen__view').srcObject = null;
  document.getElementById('myFace').srcObject = myStream;

  //화면공유UI 미노출
  removeScreenShareUI();
}


//상대방이 화면을 공유했을 때, 화면공유 이벤트를 받는 소켓 이벤트 처리
socket.on('screen_share', (changedSocketId, remoteNickname) => {
  console.log("####### 111. 새로들어온 유저가 화면공유 이벤트: %o", remoteNickname);
  //화면공유UI 노출
  screenShareView.style.display = 'flex';
  //변경된 새 비디오트랙을 가져온다.
  const video = document.getElementById(changedSocketId).children[0].srcObject;
  //스크린공유화면 미디어 정보를 돔에 넣는다.
  document.getElementById('screen__view').srcObject = video;
  //화면공유 UI로 변경
  addScreenShareUI(remoteNickname);
})

//화면공유중지 이벤트를 받음
socket.on('screen_share_stop', (changedSocketId) => {
  //화면을 공유한 소켓이 아니라, 공유 받아야할 소켓정보 
  console.log('##### 22. 원격에서 화면 공유 중지 이벤트 받음!!, 원격 소켓ID : %o', changedSocketId);

  //스크린공유화면 빈값으로 만들기
  document.getElementById('screen__view').srcObject = null;

  //화면공유UI 미노출
  removeScreenShareUI();
})

//참가인원 초과했을 경우 홈으로 이동시킴
socket.on("reject_join", () => {
  // Paint modal
  //paintModal("참가인원이 초과하여 회의에 입장할 수 없습니다!");
  location.replace(`${window.location.origin}/reject`);

  // Erase names
  const nicknameContainer = document.querySelector("#userNickname");
  nicknameContainer.innerText = "";
  roomName = "";
  nickname = "";
});


// Socket call
// 다른사람이 방에 들어올 경우 (예를들어 peer A가 받는 이벤트)
socket.on("welcome", async (userObjArr) => {
  console.log("###### join the room : %o", roomName);
  
  //같은방에 참가한 유저정보를 가져온다
  const length = userObjArr.length;
  if (length === 1) {
    return;
  }
  //firefox브라우저에서 myStream정보를 더 빨리 호출하는 문제때문에 0.6초로 설정하게됨
  setTimeout(() => {
    makePeerConnection(length, userObjArr);
  }, 600);
});

//각 피어별 RTCPeerConnection객체를 생성해 시그널링이 이뤄지도록 처리하는 함수
async function makePeerConnection(length, userObjArr) {
  //방금 입장한 유저를 제외한 이전유저들(Peer)
  for (let i = 0; i < length - 1; ++i) {
    try {
      //피어연결을 위한 내 RTCPeerConnection객체를 생성
      //최초입장한 참여자외에는 이 구문에서 PC객체를 생성하게 된다.
      const newPeerConnection = makeConnection(
        userObjArr[i].socketId,
        userObjArr[i].myNickname
      );

      /**
       * 파일전송 및 채팅을 위한 datachannel생성
       * DataChannel Offer Side 처리.
       * 3명이상의 참여자가 발생할 경우, 
       * 각 유저별 구분가능한 SocketId를 키값으로 RTCDataChannel 객체를 각각 생성해 처리 한다
       * 연결된 상대방Peer의 socketID를 키로 내 datachannel을 넣는다.
       */
      chObj[userObjArr[i].socketId] = newPeerConnection.createDataChannel('file');//파일공유 채널
      chatChObj[userObjArr[i].socketId] = newPeerConnection.createDataChannel('chat');//채팅채널
      console.log("made file datachannel");
      
      //answer side 
      //내피어 객체에서 데이터채널 이벤트를 받도록 설정.
      newPeerConnection.ondatachannel = (event) => {
        console.log('B 피어, 채널생성한 피어에서 ondatachannel 이벤트 발생!!!');
        //fileDataChannelCallback(event.channel, remoteSocketId);

        if (event.channel.label === 'file') {
          console.log('#### file 전송채널');
          fileDataChannelCallback(event.channel);
        } else if (event.channel.label === 'chat') {
          console.log('#### 채팅 전송채널');
          chatDataChannelCallback(event.channel);
        }
      };


      //offer로 전달할 SDP생성: 다른 브라우저가 참여할 수 있도록 초대장을 만드는 것
      const offer = await newPeerConnection.createOffer();
      newPeerConnection.setLocalDescription(offer);

      console.log("sent the offer");
      //socketId: 이벤트를 전달할 상대방의 socketId, nickname: 방금입장한 유저
      socket.emit("offer", offer, userObjArr[i].socketId, nickname);
      
    } catch (err) {
      console.error(err);
    }
  }
}


//원격지에서 받는 offer 이벤트, answer를 생성해 전달한다.
// screenShareValue: 화면공유중인지 구분하는 상태값
socket.on("offer", async (offer, remoteSocketId, remoteNickname, screenShareValue) => {
  console.log("received the offer, screenShareValue값 확인: %o", screenShareValue);

  //화면공유중 상태값
  isScreenShare = screenShareValue;

  //회의를 최초 입장한 유저의 PC객체
  //remoteNickname: (ex: peerB의 닉네임)
  const newPeerConnection = makeConnection(remoteSocketId, remoteNickname);

  /** 
   * DataChannel Offer Side 처리:파일전송을 하기 위해 처리
   * 1:1연결의 경우는 최초입장하는 피어가 PC객체를 생성하지 않는 구조라서, 
   * 별도로 createdChannel 생성시킴 
  */
  chObj[remoteSocketId] = newPeerConnection.createDataChannel('file');
  chatChObj[remoteSocketId] = newPeerConnection.createDataChannel('chat');
  

  /** 
   * DataChannel Answer Side 처리
   * 원격지 RTC객체 생성후, 이미 datachannel채널은 생성되었기 때문에 아래처럼 이벤트핸들러 설정
  */
  pcObj[remoteSocketId].addEventListener('datachannel', (event) => {
    console.log('A peer에서 datachannel event listening!!');
    //chObj[remoteSocketId] = event.channel;
    //const chObjValue = ;
    
    //fileDataChannelCallback(event.channel);
    if (event.channel.label === 'file') {
      console.log('#### file 전송채널');
      fileDataChannelCallback(event.channel);
      // testChannelCallback(event);
    } else if (event.channel.label === 'chat') {
      console.log('#### 채팅 전송채널');
      chatDataChannelCallback(event.channel);
    }
  })

  //전달된 offer를 setRemoteDescription 시킨다.
  newPeerConnection.setRemoteDescription(offer);
  const answer = await newPeerConnection.createAnswer();
  newPeerConnection.setLocalDescription(answer);

  console.log("sent the answer");
  socket.emit('answer', answer, remoteSocketId);
  writeChat(`[${remoteNickname}] 님이 회의에 참여했습니다.`, NOTICE_CN);
  
})



//채팅 datachannel로 메세지 이벤트 수신후 콜백 처리
function chatDataChannelCallback(chObjValue) {
  chObjValue.onmessage = (event) => {
    //내가 작성한 메세지를 상대방 브라우저의 채팅창에 노출
    const {data} = event;
    const param = JSON.parse(data);
    writeChat(param.message, MYCHAT_CN, param.remoteNickname, '', false, param.remoteProfile);
  }
}

//datachannel 이벤트 수신후 콜백 처리
function fileDataChannelCallback(chObjValue) {
  console.log('##### fileDataChannelCallback ...!!! #####: %o', chObjValue);
  

  ///채널의 기본 예상 바이너리 유형은 Blob이지만 
  //Google Chrome은 이를 지원하지 않으므로 내부적으로 binaryType을 "arraybuffer"로 설정
  // 반면에 Firefox는 Blob을 지원하므로 Blob이 필요
  // 두 브라우저 모두에서 작동하려면 채널의 바이너리 유형을 "배열 버퍼"로 선언하고 Blob을 배열 버퍼로 변환
  chObjValue.binaryType = 'arraybuffer';

  //청크(배열 버퍼)가 오는 대로 저장할 배열
  const receivedBuffers = [];

  //바이트를 받으면 다시 blob을 만들고 다운로드하고 채널을 닫는다
  chObjValue.onmessage = (event) => {

      //최초에 보낸 channel.send()이벤트를 받아서 처리하는 구문
      //전역에 선언한 receivedFile값이 없을 경우 파일정보를 먼저 넣는다.
      if(receivedFile['name'] == undefined) {
        console.log('name이 undefined일때');

        const file = JSON.parse(event.data);
        //미리 돔을 그리고 프로그레스바UI를 그릴 수 있도록 한다. 
        drawFileDownloadUI(file, false, file.remoteProfile);

        console.log(file);
        receivedFile = file;
        // progressbar.max = file.size;
        // progressbar.value = 0;
        return;
      }

      //////////////////////////////////////////////
      //파일이 chunk단위로 전송되면서 처리되는 구문

      receiveBuffer.push(event.data);
      receivedSize += event.data.byteLength;

      progressbar.value = receivedSize;

      //message를 받을때마다 파일다운로드 진행상태를 %로 나타내준다
      progressLabel.innerHTML = 'Receive: ' + (receivedSize/receivedFile.size*100).toFixed(1) + '%';

      if(receivedSize == receivedFile['size']) {
        // console.log('receivedSize가 receivedFile[size]와 동일할 때...!');

        const blob = new Blob(receiveBuffer, {type: receivedFile['type']});

        //파일을 다운받을 URL를 생성한다.
        fileDown.href = URL.createObjectURL(blob);
        fileDown.download = receivedFile['name'];

        receiveBuffer = [];
        receivedSize = 0;
        receivedFile = {};
      }
  };
}



//원격 채널에서 "메시지" 이벤트가 발생하면 handleReceiveMessage()메서드가 이벤트 핸들러로 호출
function handleReceiveMessage(event, remoteSocketId) {
  console.log(`received event! ${event.data}`);
  console.log('chObj[' +remoteSocketId+ '] : %o', chObj[remoteSocketId]);


  //파일을 받는 처리 x
  chObj[remoteSocketId].send('hi back!');
}


//원격피어 상태 변경 이벤트 수신 로그
function handleReceivedChannelStatusChange(event, remoteSocketId) {
  if (chObj[remoteSocketId]) {
    console.log("Receive channel's status has changed to " +
        chObj[remoteSocketId].readyState);
  }
}


//peer A에서 전달받은 answer이벤트
//모든 브라우저가 remote, local description을 갖게된다
socket.on('answer', async (answer, remoteSocketId) => {
  console.log("received the answer ");
  //remoteSocketId에 연결을 시도하는 내 피어객체에 상대방에게서 전달된 SDP를 넣는다.
  await pcObj[remoteSocketId].setRemoteDescription(answer);
})


// RTC Code

/** 
 * 각 피어 연결을 RTCPeerConnection객체에 의해 처리하는 함수.
 * stream정보를 내 peer에 넣는다.
 * 카메라가 바뀌면 deviceId가 바뀌기 때문에, stream도 같이 업데이트를 해준다.
 * 2명이상의 유저가 회의에 참가했을 때, 
 * 상대Peer에 대한 socketId를 키값으로 내 PC객체를 처리해 다자간 시그널링이 처리되도록 한다.
*/
function makeConnection(remoteSocketId, remotenickname) {
  console.log('1. makeConnection ...!');

  //RTCPeerConnection -> 로컬 컴퓨터와 원격 피어 간의 WebRTC 연결을 담당해주는 객체 
  //공용IP정보를 가져오기 위해 STUN서버 설정
  //장치에 공용주소를 알려주는 코드
  myPeerConnection = new RTCPeerConnection({
    iceServers: [
      {
        urls: [
      "stun:stun.l.google.com:19302",
      "stun:stun1.l.google.com:19302",
      "stun:stun2.l.google.com:19302",
      "stun:stun3.l.google.com:19302",
      "stun:stun4.l.google.com:19302",
        ],
      }
    ]
  });

  console.log('2. makeConnection ...!: %o', myPeerConnection);

  //myPeerConnection 객체 생성 후에, icecandidate 이벤트를 받도록 설정한다. 
  myPeerConnection.addEventListener("icecandidate", (e) => {
    handleIce(e, remoteSocketId);
  });

  //addICECandidate()이후 상대방 peer의 stream정보를 추가한다.
  myPeerConnection.addEventListener("addstream", (e) => {
    handleAddStream(e, remoteSocketId, remotenickname);
  });

  console.log('3. makeConnection ...!');

  //peer Connection에 오디오, 미디어 스트림을 넣는다
  myStream
    .getTracks()
    .forEach((track) => {
    //다른 유저에게 전송될 트랙들의 묶음에 신규 미디어 트랙을 추가
    myPeerConnection.addTrack(track, myStream);
  })

  console.log('4. makeConnection myStream ...! : %o', myStream);

  /**
   * RTCPeerConnection은 하나의 P2P연결만 수행하기 때문에, 
   * 3명이상의 참여자가 발생할 경우, 
   * 각 유저별 구분가능한 SocketId를 키값으로 RTCPeerConnection 각각 객체를 생성해 시그널링 처리를 한다
   * 연결된 상대방Peer의 socketID를 키로 내 피어객체를 넣는다.
   */
  pcObj[remoteSocketId] = myPeerConnection;

  ++peopleInRoom;

  console.log('5. isScreenShare ...! : %o', isScreenShare);
  sortStreams(isScreenShare);

  //내가 화면공유중 상태일 때, 새로 들어온 유저에게 화면공유 화면이 노출되도록 이벤트 발생
  if (isMyScreenShare) {
    updateMediaTrack();
    //화면공유 미디어 트랙이 곧바로 업데이트되지 않아, 0.2초 정도의 지연을 두고 화면공유 UI를 처리한다.
    setTimeout(() => {
      socket.emit('screen_share', remoteSocketId, mySocketId, myNickname);  
    }, 100);
  }
  return myPeerConnection;
}

//내가 화면공유중인 상태일때, 내 미디어 트랙으로 공유중인 화면을 업데이트하고, 
//내 닉네임으로 UI에 발표자가 보이도록 업데이트한다.
// socket.on('response_sharerNickname', (sharerNickname) => {
//   console.log('response_sharerNickname 호출!!!, sharerNickname : %o', sharerNickname);
//   //화면공유UI 노출
//   screenShareView.style.display = 'flex';
//   addScreenShareUI(sharerNickname);
// })

//새로운 회의참가자가 있을 경우 미디어 트랙을 업데이트 하는 함수.
function updateMediaTrack() {
  const count = Object.keys(pcObj).length;
  //console.log('count 갯수 확인: %o ', count);
  if (count >= 1) {
    //연결된 피어객체들을 가져온다.
    const PCObject = Object.values(pcObj);
    //Peer객체별로 미디어트랙을 전환하도록 실행
    PCObject.forEach(pcObj => {
      //getSenders(): RTCRtpSender객체의 배열을 반환
      //배열의 각 객체는 하나의 트랙의 데이터의 송신을 담당하는 RTP sender를 나타냅니다.
      //Sender 객체는 트랙 데이터의 인코딩과 송신을 확인하고, 조작 할 수 있는 메소드와 속성.
      const peerVideoSender = pcObj.getSenders()
                              .find((sender) => sender.track.kind == "video");
      //화면공유중이던 비디오 트랙으로 변경한다.
      //새로 들어온 유저에게만 업데이트 시켜야 한다.
      peerVideoSender.replaceTrack(displayMediaStream.getTracks()[0]);
    })
  }
  //로컬화면에서 공유된 화면으로 변경
  document.getElementById('screen__view').srcObject = displayMediaStream;
  document.getElementById('myFace').srcObject = displayMediaStream;
}

//브라우저가 각 각 candidate를 주고받는 이벤트를 발생시키는 코드
//연결된 상대방에 대한 socketId를 인자로 받는다(remoteSocketId)
function handleIce(data, remoteSocketId) {
  if(data.candidate){
    console.log("sent candidate");
    socket.emit('ice', data.candidate, remoteSocketId);
  }  
}

//상대방의 candidate를 전달받아 addIceCandidate를 실행한다.
socket.on('ice', async (ice, remoteSocketId) => {
  console.log("addIceCandidate...!");

  //remoteSocketId에 연결을 시도하는 내 피어객체에 상대방에게서 전달된 Ice객체를 add한다
  await pcObj[remoteSocketId].addIceCandidate(ice);
})


//연결된 상대의 브라우저 스트림 정보를 받아, 상대방 비디오를 보여주게 된다.
function handleAddStream(data, remoteSocketId, remotenickname) {
  console.log("addStream...!");
  paintPeerFace(data.stream, remoteSocketId, remotenickname);
}

//회의방에 입장한 다른유저Peer의 비디오stream정보를 받아 DOM에 그리는 함수
function paintPeerFace(peerStream, remoteSocketId, remotenickname) {
  const streams = document.querySelector(".screen");
  
  const div = document.createElement("div");
  div.id = remoteSocketId;
  const video = document.createElement("video");
  video.autoplay = true;
  video.playsInline = true;
  video.width = "400";
  video.height = "400";
  video.srcObject = peerStream; //상대방 비디오를 그린다.
  const nicknameContainer = document.createElement("p");
  nicknameContainer.id = "userNickname";

  //내가 진입했을때랑, 다른 사람이 들어왔을대 호스트구분 
  nicknameContainer.innerText = remotenickname;

  div.appendChild(video);
  div.appendChild(nicknameContainer);
  streams.appendChild(div);
  sortStreams(isScreenShare);
}


//비디오스트림UI를 변경하고 새로 정렬하기 위한 함수
function sortStreams(isScreenShare = false) {
  console.log('#### 화면공유 값 isScreenShare : %o', isScreenShare);
  
  const streams = document.querySelector(".screen");
  const streamArr = streams.querySelectorAll("div");
  
  //회의 중 새로고침 또는 회의를 나갔다가 다시 돌아왔을때 소켓커넥션이 끊기면서
  //peopleInRoom갯수가 정확히 카운트가 안되는 오류가 있어 video 돔갯수로 사용자수를 카운트함
  let videoScreen = document.querySelectorAll('.screen > div > video');
  peopleInRoom = videoScreen.length;
  console.log('#### peopleInRoom갯수 확인 : %o', peopleInRoom);

  streamArr.forEach((stream) => {
    stream.className = `people${peopleInRoom}`;
  });

  if (peopleInRoom == 1) {
    const people1Dom = document.querySelectorAll('.people1');
    people1Dom.forEach(dom => {
      if (dom.children[0]) {
        if (isScreenShare || isMyScreenShare) { //화면공유중
          dom.children[0].height = '200';
          dom.classList.add('screenShare_people1');
        } else { //화면 미공유중
          dom.children[0].height = '400';
          dom.classList.remove('screenShare_people1');
        }
      }
    })
  }

  if (peopleInRoom == 2) {
    const people2Dom = document.querySelectorAll('.people2');
    people2Dom.forEach(dom => {
      if (dom.children[0]) {
        if (isScreenShare) { //화면공유중
          //화면공유의 경우 클래스 속성추가
          streams.classList.add('share');
          streams.style.height = 'calc(100% - 550px)';

          dom.children[0].width = '200';
          dom.children[0].height = '200';
        } else { //화면 미공유중

          //화면공유가 아닐경우 클래스 속성 제외
          streams.classList.remove('share');
          streams.style.height = 'calc(100% - 81px)';

          dom.children[0].width = '400';
          dom.children[0].height = '400';
        }
      }
    })
  }
  
  if (peopleInRoom == 3) {
    const people3Dom = document.querySelectorAll('.people3');
    people3Dom.forEach(dom => {
      if (dom.children[0]) {
        if (isScreenShare) { //화면공유중

          //화면공유의 경우 클래스 속성추가
          streams.classList.add('share');
          streams.style.height = 'calc(100% - 420px)';

          dom.children[0].width = '200';
          dom.children[0].height = '200';
          //dom.style.marginTop = 0 + 'px';
          dom.classList.add('screenShare');

        } else { //화면 미공유중

          //화면공유가 아닐경우 클래스 속성 제외
          streams.classList.remove('share');
          streams.style.height = 'calc(100% - 81px)';

          dom.children[0].width = '400';
          dom.children[0].height = '300';
          //dom.style.marginTop = -8 + 'em';
          dom.classList.remove('screenShare');
        }
      }
    })
  }

  //4명 이상인 경우 화면공유 CSS처리 및 테스트 못함
  if (peopleInRoom == 4) {
    const people4Dom = document.querySelectorAll('.people4');
    people4Dom.forEach(dom => {
      dom.children[0].height = '370';  
    })
  }

  if (peopleInRoom == 5) {
    const people5Dom = document.querySelectorAll('.people5');
    people5Dom.forEach(dom => {
      dom.children[0].height = '370';
      dom.children[0].height = '350';  
    })
  }
}


/** 채팅영역 */
const chatForm = document.querySelector(".chatForm");
const chatBox = document.querySelector(".chat__box");

const MYCHAT_CN = "myChat";
const NOTICE_CN = "noticeChat";

//send버튼 클릭후 submit액션

chatForm.addEventListener("submit", handleChatSubmit);

//채팅내용을 datachaneel로 전송하는 함수
function handleChatSubmit(event) {
  //console.log('채팅 datachannel 전송 event call...!');
  event.preventDefault();

  const chatInput = chatForm.querySelector("input");
  const message = chatInput.value;
  chatInput.value = "";

  if (message) {
    //파일업로드DOM을 한번만 그리기 위한 구분값
    let isFrist = true;

    const count = Object.keys(pcObj).length;
    console.log(`count값 확인 : %o`, count);
    
    if (count === 0) { //한명만 접속했을 경우
      //내가 작성한 메세지를 내 채팅창에 노출
      writeChat(`${message}`, MYCHAT_CN, nickname, '', { mine: true}, profile);
    } else if (count >= 1) {
      const channels = Object.values(chatChObj);
      channels.forEach(channel => {
        sendChat(channel, message, isFrist, myId);
        isFrist = false;
      })
    }
  }
}

/**
 * datachannel로 채팅전송 
 * @param {*} channel datachannel 객체
 * @param {*} message 채팅메세지
 * @param {*} isFrist 접속피어가1명인지 구분하는 값
 * @param {*} myId 모바일에서 호스트여부를 구분하기 위해 전달하는 접속ID
 */
function sendChat(channel, message, isFrist, myId) {
  if (channel.readyState === 'open') {
    
    let remoteNickname = '';
    if (nickname.includes('(호스트)')) {
      remoteNickname = nickname.replace('(호스트)', '');
    } else {
      remoteNickname = nickname;
    }

    const param = {
      message: message,
      remoteNickname: remoteNickname,
      remoteProfile: profile,
      remoteId: myId
    }
    channel.send(JSON.stringify(param));
  }

  //본인화면에서 채팅UI그리도록 처리
  if (isFrist) {
    //내가 작성한 메세지를 내 채팅창에 노출
    writeChat(`${message}`, MYCHAT_CN, nickname, '', { mine: true}, profile);
  }
}


//채팅내용을 socket서버로 전송하는 함수
function handleChatSubmit2(event) {
  console.log('채팅 send 버튼 클릭 후 submit event call...!');
  event.preventDefault();
  const chatInput = chatForm.querySelector("input");
  const message = chatInput.value;
  chatInput.value = "";

  //소켓서버에 채팅내용, 채팅을 작성한 유저명, 방번호, 시간을 전송
  const time = moment(new Date()).format("h:mm A") //현재시간 오전/오후
  socket.emit("chat", message, roomName, nickname, time);
  console.log('emit chat event...!');

  //내가 작성한 메세지를 내 채팅창에 노출
  writeChat(`${message}`, MYCHAT_CN, nickname, '', { mine: true}, profile);
}

//채팅내용을 가져와 채팅LI노드를 만들어 화면에 그린다
function writeChat(message, className = null, 
    nickname = null, time = null, mine = false, profile = null) {

  time = moment(new Date()).format("h:mm A") //현재시간 오전/오후

  const li = document.createElement("li");

  //입장,나가기 노티 메세지UI 그리기
  if (className === NOTICE_CN) {
    const span = document.createElement("span");
    span.innerText = message;
    li.appendChild(span);
    li.classList.add(className);

  } else {
    if(mine){
      nickname = `${nickname}[나]`;
    }

    let profileImg = ''; 
    //console.log('>>>>> profile값 확인 : %o', profile.trim());
    if(profile) {
      profileImg = `${API_SERVER_URL}/api/member/profile/${profile.trim()}`;
    } else {
      profileImg = `${window.location.origin}/public/image/default_user.png`;
    }

    //일반 채팅메세지UI 그리기
    li.innerHTML = `<li>
        <span class="profile">
          <img class="image" src=${profileImg} alt="any" srcset="">
        </span>
        <div class="message__info">
          <span class="user ${mine ? 'mine': ''}">${nickname}</span>
          <span class="message">${message}</span>
          <span class="time">${time}</span>
        </div>
      </li>`;

  }

  //첫 번째 자식 앞에 Node개체 또는 개체 집합을 삽입(chatBox다음에 li노드가 나온다)
  chatBox.appendChild(li);

  //채팅입력 후 채팅박스의 스크롤을 하단으로 내린다.
  chatBox.scrollTo(0, chatBox.scrollHeight + 200);    

}

socket.on("chat", (message, remoteNickname, time) => {
  console.log('chat event received from server...!');
  //내가 작성한 메세지를 상대방 브라우저의 채팅창에 노출
  writeChat(message, MYCHAT_CN, remoteNickname, time);
});

//채팅내용 클리어
function clearAllChat() {
  //오류
  // const chatArr = chatBox.querySelectorAll("li");
  // chatArr.forEach((chat) => chatBox.removeChild(chat));
}

// 파일공유

//버튼 클릭후 파일공유 화면이 뜬다.
//파일을 선택하면 해당 파일을 불러온다. 

//하단 파일공유 버튼 
const shareFileBtn = document.querySelector('.fileShare');
const cancleBtn = document.getElementById('cancel-button');
const selectFileInput = document.getElementById('select-file-input');

shareFileBtn.addEventListener('click', () => {
  document.getElementById('select-file-dialog').style.display = 'block';
});

document.getElementById('select-file-input').addEventListener('change', (event) => {
  file = event.target.files[0];
  document.getElementById('ok-button').disabled = !file;
});

cancleBtn.addEventListener('click', () => {
  document.getElementById('select-file-input').value = '';
  document.getElementById('select-file-dialog').style.display = 'none';
});

selectFileInput.addEventListener('change', (event) => {
  file = event.target.files[0];
  document.getElementById('ok-button').disabled = !file;
});


//파일을 전송을 위해, 피어간 데이터채널 객체를 분기처리 하는 함수
const shareFile = () => {
  if (file) {
    //파일업로드DOM을 한번만 그리기 위한 구분값
    let isFrist = true;

    const count = Object.keys(pcObj).length;

    if (count === 0) {//한명만 접속했을 경우

      createSelfFlieDownloadDom(file);

    } else if (count >= 1) {
      const channels = Object.values(chObj);
      channels.forEach(channel => {
        doShareFile(channel, isFrist);
        isFrist = false;
      })
    }
  }
};


//파일전송을 처리 함수
function doShareFile(channel, isFrist) {
  channel.binaryType = 'arraybuffer';
  //"낮음(low)"으로 간주되는 버퍼링된 나가는 데이터의 바이트 수를 지정합니다. 기본값은 0입니다.
  channel.bufferedAmountLowThreshold = 0;

  //본인화면에서 파일업로드 돔그리도록 처리 -> 한번만 그리도록 변경
  if (isFrist) {
    //createSelfFlieDownloadDom(file.name)
    //미리 돔을 그려서 프로그레스바UI를 그릴 수 있도록 한다. 
    drawFileDownloadUI(file, true, profile);
  }

  if (channel.readyState === 'open') {
    //파일전송
    console.log('send file date...! : %o', file);

    channel.send(JSON.stringify({
        name: file.name,
        size: file.size,
        type: file.type,
        remoteProfile: profile
    }));

    sendData(channel, isFrist);

    closeDialog();
    
  }

  channel.onmessage = (event) => {
    console.log('채널생성한 피어) onmessage event call,,,! : %o', event.data);
  }

}

//파일을 arrayBuffer로 변환해 chunck시켜 데이터채널로 전송하는 함수
function sendData(dataChannel, isFrist) {
  console.log('sendData 호출....!');

  let offset = 0; //전송된 파일사이즈 표기변수

  //브라우저간 데이터메세지 전송 제한을 고려해 16KB로 chunk사이즈 지정
  let maxChunkSize = 16384;
  // progressbar.value = 0; //프로그래스바 초기화
  
  console.log(dataChannel.bufferedAmountLowThreshold);
  let receivedBuffers = [];

  //1.Blob 메서드 arrayBuffer 를 사용하여 파일을 배열 버퍼로 변경
  file.arrayBuffer().then(buffer => {

      const send = () => {
          //buffer사이즈만큼 반복문을 돈다
          while (buffer.byteLength) {

              
              //로그확인시, bufferAmount의 바이트값이 16384(16KB)가 될때마다,
              //onbufferedamountlow이벤트 발생시키도록 처리되고, chunk를 전송 할때마다 반복실행됨
              // console.log('dataChannel.bufferedAmountLowThreshold : %o', dataChannel.bufferedAmountLowThreshold); //0 
              // console.log('dataChannel.bufferedAmount : %o', dataChannel.bufferedAmount);//16384

              /**
               * 
               * dataChhannel.BufferedAmount() get's full 이슈를 해결하기 위한 코드
               * dataChannel bufferedAmount가 16Kib 아래로 감소할 때 수신 대기할 콜백 수신기를 구현
               * 
               * - bufferedAmount: 데이터 채널을 통해 전송되기 위해 현재 큐에 있는 데이터의 바이트 수를 반환
               * - bufferedAmountLowThreshold: "낮음(low)"으로 간주되는 버퍼링된 나가는 데이터의 바이트 수를 지정합니다. 기본값은 0입니다.
              */
              if (dataChannel.bufferedAmount > dataChannel.bufferedAmountLowThreshold) {
                  //현재 아웃바운드 데이터 전송 버퍼에 있는 바이트 수가 에 지정된 임계값 아래로 떨어지면 발생되는 이벤트
                  dataChannel.onbufferedamountlow = () => {
                      
                      dataChannel.onbufferedamountlow = null;
                      send();
                      //console.log('onbufferedamountlow 이벤트 발생!!!!, onbufferedamountlow를 null로 만들고, send()함수를 실행시킴');
                  };
                  return;
              }
              
              //2.전체 파일이 올 때까지 청크를 하나씩 보낸다.
              const chunk = buffer.slice(0, maxChunkSize);
              //로컬에서도 파일을 다운받기 위해 localReceivedBuffer에도 chunk를 넣는다.
              receivedBuffers.push(chunk);
              //3. 전송된 size만큼 자른뒤 배열 업데이트.
              buffer = buffer.slice(maxChunkSize, buffer.byteLength);

              //chunk시킨 데이터를 send
              dataChannel.send(chunk);

              offset += maxChunkSize;
              //데이터를 얼만큼 보냈는지 누적된 size값을 로그로남긴다
              //console.log('Sent ' + offset + ' bytes.');

              //전송된 파일사이즈 양을 프로그레스바에 표시한다.
              progressbar.value = offset >= file.size ? file.size : offset;

              //1) 파일보내기 성공후 성공했다는 메세지가 뜨도록 처리
              //2) 로컬에서도 다운받을 수 있는UI를 그리도록 처리 
              if (offset >= file.size && isFrist) {
                const blob = new Blob(receivedBuffers, {type: file.type});
                //파일을 다운받을 URL를 생성한다.
                fileDown.href = URL.createObjectURL(blob);
                fileDown.download = file.name;

                showAlert('success');
              }
              
              //프로그래스바 파일전송상태를 %로 나타냄
              progressLabel.innerHTML = offset >= file.size ? '보내기 완료' : (offset/file.size*100).toFixed(1) + '%';
          }
      };

      send();

  })

}

/**
 * 로컬피어에밖에 없을 경우, 파일을 업로드해도 다운받을 수 있도록 하는 함수
 */
function createSelfFlieDownloadDom(file) {
  console.log('#### createSelfFlieDownloadDom start!');

  //파일업로드후 팝업창은 닫는다. 
  closeDialog();

  try {

    //미리 돔을 그려서 프로그레스바UI를 그릴 수 있도록 한다. 
    drawFileDownloadUI(file, true, profile);

    let offset = 0; //전송된 파일사이즈 표기변수
    let maxChunkSize = 16384; //16KB
    let receivedBuffers = []; //chunk를 넣을 배열선언
    
    //1.Blob 메서드 arrayBuffer 를 사용하여 파일을 배열 버퍼로 변경
    file.arrayBuffer().then(buffer => {
      //buffer사이즈만큼 반복문을 돈다
      while (buffer.byteLength) {
        //2.전체 파일이 올 때까지 청크를 하나씩 보낸다.
        const chunk = buffer.slice(0, maxChunkSize);
        //로컬에서도 파일을 다운받기 위해 localReceivedBuffer에도 chunk를 넣는다.
        receivedBuffers.push(chunk);
        //console.log('chunk생성...!');

        //3. 전송된 size만큼 자른뒤 배열 업데이트.
        buffer = buffer.slice(maxChunkSize, buffer.byteLength);
        offset += maxChunkSize;
        //전송된 파일사이즈 양을 프로그레스바에 표시한다.
        progressbar.value = offset >= file.size ? file.size : offset;

        //프로그래스바 파일전송상태를 %로 나타냄
        progressLabel.innerHTML = offset >= file.size ? '보내기 완료' : (offset/file.size*100).toFixed(1) + '%';

        //파일보내기 성공후 성공했다는 메세지가 뜨고, 다운로드 링크 설정
        if (offset >= file.size) {
          const blob = new Blob(receivedBuffers, {type: file.type});
          //파일을 다운받을 URL를 생성한다.
          fileDown.href = URL.createObjectURL(blob);
          fileDown.download = file.name;

          showAlert('success')
        }
      }
    })
    
  } catch (error) {
    console.error(error);
    closeDialog();
    showAlert('fail');
  }
  
}


//파일전송 성공/실패 팝업 노출 
function showAlert(state) {
  const alert = document.querySelector(`.alert.alert-${state}`);
  alert.style.display = 'block';
  setTimeout(() => {
    alert.style.display = 'none';
  }, 2500);
}


//파일전송버튼 클릭 후 파일을 다른파일에 전송하는 처리
document.getElementById('ok-button').addEventListener('click', () => {
  shareFile();
});

const closeDialog = () => {
  document.getElementById('select-file-input').value = '';
  document.getElementById('select-file-dialog').style.display = 'none';
};


//샘플 파일다운로드 
const downloadFile2 = (blob, fileName) => {
    const a = document.createElement('a');
    const url = window.URL.createObjectURL(blob);
    a.href = url;
    a.download = fileName;
    a.click();
    window.URL.revokeObjectURL(url);
    a.remove()
};


//파일다운로드 URL 생성
const downloadFile = (blob, fileName, nickname, mine = false) => {
  
  //띄어쓰기 포함해서 파일전송하는 방법
  const downloadDom = `download="${fileName}"`;
  console.log('downloadDom: %o', downloadDom);


  //const a = document.createElement('a');
  //주어진 객체를 가리키는 URL을 DOMString 으로 반환한다.

  const url = window.URL.createObjectURL(blob);

  const chatBox = document.querySelector('.chat__box');
  const li = document.createElement("li");
  const time = moment(new Date()).format("h:mm A") //현재시간 오전/오후


  // 채팅화면처럼 다운로드 화면 그리기
  let myNickname = '';
  if(mine){
    myNickname = `${nickname}[나]`;
  }

  //미로그인의 경우 디폴트 이미지가 노출되도록 처리
  let profileImg = '';
  if(isLogin) {
    profileImg = `${API_SERVER_URL}/api/member/profile/${localStorage.getItem('profile')}`;
  } else {
    profileImg = `${window.location.origin}/public/image/default_user.png`;
  }

  //일반 채팅메세지UI 그리기
  li.innerHTML = `<li>
      <span class="profile">
        <img class="image" src=${profileImg} alt="any" srcset="">
      </span>
      <div class="message__info">
        <span class="user ${mine ? 'mine': ''}">${mine? myNickname : nickname}</span>
        <span class="message file">
        <i class="fas fa-file fa-2x"></i>
          <a class="file__down" ${downloadDom}
            href=${url}>${fileName}</a>
          <span class="file__down-info">-클릭후 파일열기</span>
        </span>

        <span class="time">${time}</span>
      </div>
    </li>`;
  console.log('li값 확인 : %o', li);

  chatBox.appendChild(li);
  //a.click();
  
  //객체URL을 해제한다
  //window.URL.revokeObjectURL(url);
  //a.remove()
};


//파일다운로드 UI DOM을 그리는 함수
function drawFileDownloadUI(file, mine = false, profile = null) {

  const chatBox = document.querySelector('.chat__box');
  const li = document.createElement("li");
  const time = moment(new Date()).format("h:mm A") //현재시간 오전/오후

  // 채팅화면처럼 다운로드 화면 그리기
  let myNickname = '';
  if(mine){
    myNickname = `${nickname}[나]`;
  }

  //미로그인의 경우 디폴트 이미지가 노출되도록 처리
  let profileImg = ''; 
  if(profile) {
    profileImg = `${API_SERVER_URL}/api/member/profile/${profile}`;
  } else {
    profileImg = `${window.location.origin}/public/image/default_user.png`;
  }

  //프로그레스바 ID에 구분값을 주기위해 현재시간을 ID에 같이표기한다.
  const timeValue = Math.round(new Date().getTime() / 1000);

  //일반 채팅메세지UI 그리기
  li.innerHTML = `<li>
      <span class="profile">
        <img class="image" src=${profileImg} alt="any" srcset="">
      </span>
      <div class="message__info">
        <span class="user ${mine ? 'mine': ''}">${mine? myNickname : nickname}</span>
        <span class="message file">
        <i class="fas fa-file fa-2x"></i>
          <a class="file__down file_${timeValue}" download=""
            href="">${file.name}</a>
          <span class="file__down-info">-클릭후 파일열기</span>
          <progress id="progress_${timeValue}" value="0"></progress>&nbsp;&nbsp;
          <label id="progressLabel_${timeValue}"></label>
        </span>

        <span class="time">${time}</span>
      </div>
    </li>`;

  chatBox.appendChild(li);

  progressbar = document.getElementById(`progress_${timeValue}`);
  progressLabel = document.getElementById(`progressLabel_${timeValue}`);
  fileDown = document.querySelector(`.file__down.file_${timeValue}`);

  //프로그래스바 태그에 요소가 나타내는 작업에 필요한 작업량속성을 준다.
  //ex: max="100" value="70"이면 프로그래스바에 70%정도로 표기됨
  progressbar.max = file.size;
  //요소가 나타내는 작업을 완료한 양
  progressbar.value = 0;

  //프로그래스 상태값을 나타내주는 라벨값 (0~100%)
  progressLabel.innerHTML = 0;

}
