const socket = io();
//const moment = require(); 
//import moment from '../../'

/** 회의방입장 */
const enterRoom = document.querySelector('.enterRoom');

/** 비디오스트림 */
const meetingMain = document.querySelector('.meetingMain');
const myFace = document.getElementById("myFace");


/** 컨트롤영역 */
const muteBtn = document.getElementById("mute");
const muteIcon = muteBtn.querySelector(".muteIcon");//음소거
const unMuteIcon = muteBtn.querySelector(".unMuteIcon");//음소서 비활성화

const cameraBtn = document.querySelector("#camera");
const cameraIcon = cameraBtn.querySelector(".cameraIcon");
const unCameraIcon = cameraBtn.querySelector(".unCameraIcon");
const camerasSelect = document.getElementById("cameras");
const leaveBtn = document.getElementById('leave');


//css hidden속성
const HIDDEN_CN = "hidden";

let roomName = "";
let nickname = "";
//회의방에 있는 참가자수 초기화
let peopleInRoom = 1;

let myStream;
let muted = true;
let cameraOff = false;
let myPeerConnection;
//let newDataChannel;
let newDataChannel;


//홈화면 입장전에는 미팅화면 보이지 않음
meetingMain.classList.add(HIDDEN_CN);

//음소거 비활성화 아이콘 미노출
unMuteIcon.classList.add(HIDDEN_CN);

//카메라 비활성화 아이콘 미노출
unCameraIcon.classList.add(HIDDEN_CN);

//3명이상의 참여자가 발생할 경우,  
//각 유저별 SocketId를 키값으로 RTCPeerConnection객체를 생성해서 관리하기 위한 객체선언
let pcObj = {
  // remoteSocketId: pc
};

/**
 * 1)native로 부터 방번호,호스트여부 및 음소거 정보 전달받아야 함
 * 2)데탑앱으로부터 방번호,호스트여부 및 음소거 정보 전달받아야 함
 */

//방에입장
document.addEventListener('load', () => {
  console.log('document load event call...!');
  meetingMain.style.display = '';
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
  //socket 연결을끊난다.
  socket.disconnect();

  enterRoom.classList.remove(HIDDEN_CN); //방입장화면 노출  
  meetingMain.classList.add(HIDDEN_CN); //회의화면 미노출

  welcome.hidden = false;

  //peerConnectionObjArr = [];
  peopleInRoom = 1;
  nickname = "";

  myStream.getTracks().forEach((track) => track.stop());
  const nicknameContainer = document.querySelector("#userNickname");
  nicknameContainer.innerText = "";

  myFace.srcObject = null;
  //비디오 스트림 화면 삭제
  clearAllVideos();

  //채팅내용 클리어
  //clearAllChat();
}

//socket서버의 leave_room 이벤트를 받고 회의방 나가기 처리
socket.on("leave_room", (leavedSocketId, nickname) => {
  removeVideo(leavedSocketId);
  writeChat(`[${nickname}] 님이 회의를 나갔습니다.`, NOTICE_CN);
  --peopleInRoom;
  sortStreams();
});

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
const modalBtn = modal.querySelector(".modal__btn");

function paintModal(text) {
  modalText.innerText = text;
  modal.classList.remove(HIDDEN_CN);

  modal.addEventListener("click", removeModal);
  modalBtn.addEventListener("click", removeModal);
  document.addEventListener("keydown", handleKeydown);
}

//삭제버튼 클릭후 모달삭제
function removeModal() {
  modal.classList.add(HIDDEN_CN);
  modalText.innerText = "";
  //팝업이 뜨고, 이전화면으로 넘어가야 한다.
  // meetingMain.classList.add(HIDDEN_CN);
  // enterRoom.classList.remove(HIDDEN_CN);
}

// Welcom Form (join a room)

const welcome = document.getElementById("welcome");
const welcomeForm = welcome.querySelector("form");

welcomeForm.addEventListener("submit", handlWelcomSubmit)

//회의들어가 버튼입력(submit)후 실행되는 함수
async function handlWelcomSubmit(event){
  event.preventDefault();

  if (socket.disconnected) {
    socket.connect();
  }

  enterRoom.classList.add(HIDDEN_CN); //방입장화면 미노출  
  meetingMain.classList.remove(HIDDEN_CN); //회의화면노출
  
  const inputRoom = welcomeForm.querySelector("input#roomName");
  const inputName = welcomeForm.querySelector("input#nickname");
  
  //media정보를 요청해, 방에 입장한 peer의 Stream정보를 가져와 비디오 및 카메라 정보를 그린다.  
  await initCall();

  //방에 입장했다는 이벤트를 emit하고, 'welcome'이벤트를 받을 준비를 하게 된다.
  //socket event를 emit하지 않음...!
  socket.emit('join_room', inputRoom.value, inputName.value);
  writeChat(`[${inputName.value}] 님이 회의에 참여했습니다.`, NOTICE_CN);

  roomName = inputRoom.value;
  nickname = inputName.value;

  //유저닉네임 셋팅
  const nicknameValue = document.querySelector("#userNickname");
  nicknameValue.innerText = nickname;

  inputRoom.value = "";
  inputName.value = "";
}

//화상통화 init함수
async function initCall() { 
  await newGetMedia();
  newMakeConnection();
}


async function newGetMedia(deviceId) {
  const initialConstrains = {
    audio: true,
    video: { facingMode: "user" },
  };
  const cameraConstraints = {
    audio: true,
    video: { deviceId: { exact: deviceId } },
  };
  try {
    myStream = await navigator.mediaDevices.getUserMedia(
      deviceId ? cameraConstraints : initialConstrains
    );
    myFace.srcObject = myStream;
    if (!deviceId) {
      await getCameras();
    }
  } catch (e) {
    console.log(e);
  }
}

//방에 입장하게 되면 각 피어에 ICE서버 설정을 하게 된다.
//실제 연결을 만드는 코드
function newMakeConnection() {
  console.log('makeConnection 호출 !!!');
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
    ],
  });

  //애뮬레이터에서 실행안됨
  myPeerConnection.addEventListener("icecandidate", (data) => {
    console.log('icecandidate 이벤트 발생!!!!, sent candidate');
    //console.log("sent candidate");
    socket.emit("ice", data.candidate, roomName);
  });

  myPeerConnection.onicecandidateerror = function(event) {
    console.log('answer이후 onicecandidate error event발생! : %o', event);
  }

  myPeerConnection.addEventListener("addstream", handleAddStream);

  //audio, video트랙을 peer에 넣는다. 
  // myStream
  //   .getTracks()
  //   .forEach((track) => {
  //     console.log('makeConnection 호출 이후 track값 확인 : %o', track);
  //     myPeerConnection.addTrack(track, myStream)
  //   });
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
    myFace.muted = true;

    if (!deviceId) {
      // mute default
      //TODO:오디오 어디서 가져오지??
      console.log('getAudioTracks : %o', myStream.getAudioTracks());
      myStream.getAudioTracks().forEach((track) => {(track.enabled = true); });
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

socket.on("reject_join", () => {
  // Paint modal
  paintModal("참가인원이 초과하여 회의에 입장할 수 없습니다!");

  // Erase names
  const nicknameContainer = document.querySelector("#userNickname");
  nicknameContainer.innerText = "";
  roomName = "";
  nickname = "";
});

// Socket call
// 다른사람이 방에 들어올 경우 (예를들어 peer A가 받는 이벤트)
socket.on("welcome", async (userObjArr) => {
  console.log("join the room : %o", roomName);

  //같은방에 참가한 유저정보를 가져온다
  const length = userObjArr.length;
  // if (length === 1) {
  //   return;
  // }

  console.log(`회의에 입장한 유저수: ${length}`);
  //방금 입장한 유저를 제외한 이전유저들(Peer)
  for (let i = 0; i <= length - 1; ++i) {
    try {
      console.log(userObjArr[i]);

      //피어연결을 위한 내 RTCPeerConnection객체를 생성
      //최초입장한 참여자외에는 이 구문에서 PC객체를 생성하게 된다.
      const newPeerConnection = makeConnection(
        userObjArr[i].socketId,
        userObjArr[i].nickname
      );
      
      //DataChannel - offer를 하기 전에, data channel을 만들어야 한다. 
      //원격피어(remotepeer)에서 데이터 채널을 만들고 네트워크를 통해 서로 연결하도록 설정
      newDataChannel = newPeerConnection.createDataChannel("chat");

      //각각의 피어에 message 이벤트를 받도록 처리
      newDataChannel.addEventListener("message", (e) => {
        paintChatUI(e.data, 'other', '');
      });
      console.log("made data channel");
      
      
      //offer로 전달할 SDP생성: 다른 브라우저가 참여할 수 있도록 초대장을 만드는 것
      const offer = await newPeerConnection.createOffer();
      newPeerConnection.setLocalDescription(offer);

      // console.info(`newPeerConnection.signalingState : ${newPeerConnection.signalingState}`); //stable
      // console.info(`newPeerConnection.iceConnectionState : ${newPeerConnection.iceConnectionState}`); //new
      // console.info(`newPeerConnection.iceGatheringState : ${newPeerConnection.iceGatheringState}`); //new


      console.log("sent the offer");
      //socketId: 이벤트를 전달할 상대방의 socketId, nickname: 방금입장한 유저
      socket.emit("offer", offer, userObjArr[i].socketId, nickname);

    } catch (err) {
      console.error(err);
    }
  }


})

//원격지에서 받는 offer 이벤트, answer를 생성해 전달한다.
socket.on("offer", async (offer, remoteSocketId, remoteNickname) => {
  console.log("received the offer");

  //회의를 최초 입장한 유저의 PC객체
  //remoteNickname: (ex: peerB의 닉네임)
  const newPeerConnection = makeConnection(remoteSocketId, remoteNickname);

  //datachannel 이벤트 핸들링
  newPeerConnection.addEventListener("datachannel", (e) => {
    //datachannel정의
    newDataChannel = e.channel;
    //각각의 피어에 message 이벤트를 받도록 처리
    newDataChannel.addEventListener('message', (e) => {
      console.log(e.data);
      paintChatUI(e.data, remoteNickname, 'other');
    })
  });

  //전달된 offer를 setRemoteDescription 시킨다.
  newPeerConnection.setRemoteDescription(offer);
  const answer = await newPeerConnection.createAnswer();
  newPeerConnection.setLocalDescription(answer);


  console.log("sent the answer");
  socket.emit('answer', answer, remoteSocketId);
  writeChat(`[${remoteNickname}] 님이 회의에 참여했습니다.`, NOTICE_CN);
  
})

//peer A에서 전달받은 answer이벤트
//모든 브라우저가 remote, local description을 갖게된다
socket.on('answer', async (answer, remoteSocketId) => {
  console.log("received the answer : %o", answer, remoteSocketId);
  //remoteSocketId에 연결을 시도하는 내 피어객체에 상대방에게서 전달된 SDP를 넣는다.
  await pcObj[remoteSocketId].setRemoteDescription(answer);
})


// RTC Code

/** 
 * 각 피어 연결을 RTCPeerConnection객체에 의해 처리하는 함수.
 * stream정보를 내 peer에 넣는다.
 * 카메라가 바뀌면 deviceId가 바뀌기 때문에, stream도 같이 업데이트를 해준다.
 * 2명이상의 유저가 회의에 참가했을때, 
 * 상대Peer에 대한 socketId를 키값으로 내 PC객체를 처리해 다자간 시그널링이 처리되도록 한다.
*/
function makeConnection(remoteSocketId, remotenickname) {

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

  //myPeerConnection 객체 생성 후에, icecandidate 이벤트를 받도록 설정한다. 
  myPeerConnection.addEventListener("icecandidate", (e) => {
    handleIce(e, remoteSocketId);
  });

  //addICECandidate()이후 상대방 peer의 stream정보를 추가한다.
  myPeerConnection.addEventListener("addstream", (e) => {
    handleAddStream(e, remoteSocketId, remotenickname);
  });
  
  //peer Connection에 오디오, 미디어 스트림을 넣는다
  //test 주석처리
  myStream
    .getTracks()
    .forEach((track) => {
    console.log('makeConnection 호출 이후 track값 확인 : %o', track);
    //다른 유저에게 전송될 트랙들의 묶음에 신규 미디어 트랙을 추가
    myPeerConnection.addTrack(track, myStream);
  })
  console.log('myStream.getTracks() : %o', myStream.getTracks());

  /**
   * RTCPeerConnection은 하나의 P2P연결만 수행하기 때문에, 
   * 3명이상의 참여자가 발생할 경우, 
   * 각 유저별 구분가능한 SocketId를 키값으로 RTCPeerConnection 각각 객체를 생성해 시그널링 처리를 한다
   * 연결된 상대방Peer의 socketID를 키로 내 피어객체를 넣는다.
   */
  pcObj[remoteSocketId] = myPeerConnection;
  

  ++peopleInRoom;
  sortStreams();
  return myPeerConnection;
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
  nicknameContainer.innerText = remotenickname;

  div.appendChild(video);
  div.appendChild(nicknameContainer);
  streams.appendChild(div);
  sortStreams();
}


//비디오스트림UI를 정렬하기 위한 클래스명추가 함수
function sortStreams() {
  const streams = document.querySelector(".screen");
  const streamArr = streams.querySelectorAll("div");
  //console.log(`peopleInRoom값 확인 : ${peopleInRoom}`);
  streamArr.forEach((stream) => (stream.className = `people${peopleInRoom}`));
}


/** 채팅영역 */
// const sendBtn = document.querySelector('.send');
// const message = document.querySelector('.message');

const chatForm = document.querySelector(".chatForm");
const chatBox = document.querySelector(".chat__box");

const MYCHAT_CN = "myChat";
const NOTICE_CN = "noticeChat";

//send버튼 클릭후 submit액션
chatForm.addEventListener("submit", handleChatSubmit);

//채팅내용을 socket서버로 전송하는 함수
function handleChatSubmit(event) {
  event.preventDefault();
  const chatInput = chatForm.querySelector("input");
  const message = chatInput.value;
  chatInput.value = "";

  //소켓서버에 채팅내용, 채팅을 작성한 유저명, 방번호, 시간을 전송
  const time = moment(new Date()).format("h:mm A") //현재시간 오전/오후
  socket.emit("chat", message, roomName, nickname, time);

  //내가 작성한 메세지를 내 채팅창에 노출
  writeChat(`${message}`, MYCHAT_CN, nickname, time, { mine: true});
}

//채팅내용을 가져와 채팅LI노드를 만들어 화면에 그린다
function writeChat(message, className = null, nickname = null, time = null, mine = false) {
  console.log('writeChat 호출...!');

  const li = document.createElement("li");

  //입장,나가기 노티 메세지UI 그리기
  if (className === NOTICE_CN) {
    const span = document.createElement("span");
    span.innerText = message;
    li.appendChild(span);
    li.classList.add(className);

  } else {
    //test
    if(mine){
      nickname = `${nickname}[나]`;
    }

    //일반 채팅메세지UI 그리기
    li.innerHTML = `<li>
        <span class="profile">
          <img class="image" src="https://placeimg.com/50/50/any" alt="any" srcset="">
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

}

socket.on("chat", (message, remoteNickname, time) => {
  //내가 작성한 메세지를 상대방 브라우저의 채팅창에 노출
  writeChat(message, MYCHAT_CN, remoteNickname, time);
});


