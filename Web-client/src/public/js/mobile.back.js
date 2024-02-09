const socket = io();

/** 모바일용 */


/** 회의방입장 */
const enterRoom = document.querySelector('.enterRoom');


/** 비디오스트림 */
const meetingMain = document.querySelector('.meetingMain');
const myFace = document.getElementById("myFace");


/** 컨트롤영역 */
// const muteBtn = document.getElementById("mute");
// const muteIcon = muteBtn.querySelector(".muteIcon");//음소거
// const unMuteIcon = muteBtn.querySelector(".unMuteIcon");//음소서 비활성화

// const cameraBtn = document.querySelector("#camera");
// const cameraIcon = cameraBtn.querySelector(".cameraIcon");
// const unCameraIcon = cameraBtn.querySelector(".unCameraIcon");
const camerasSelect = document.getElementById("cameras");

//홈화면 입장전에는 미팅화면 보이지 않음
meetingMain.style.display = 'block';

//css hidden속성
const HIDDEN_CN = "hidden";

let myStream;
let muted = true;
let cameraOff = false;
let myPeerConnection;

//음소거 비활성화 아이콘 미노출
// unMuteIcon.classList.add(HIDDEN_CN);

//카메라 비활성화 아이콘 미노출
// unCameraIcon.classList.add(HIDDEN_CN);


let nickname = "";
let peopleInRoom = 1;

//3명이상의 참여자가 발생할 경우,  
//각 유저별 SocketId를 키값으로 RTCPeerConnection객체를 생성해서 관리하기 위한 객체선언
let pcObj = {
  // remoteSocketId: pc
};

/**
 * 1)native로 부터 방번호,호스트여부 및 음소거 정보 전달받아야 함
 *  -> 어떻게?
 * 2)데탑앱으로부터 방번호,호스트여부 및 음소거 정보 전달받아야 함
 *  -> 어떻게?
 */

//방에입장
document.addEventListener('load', () => {
  console.log('document load event call...!');
  meetingMain.style.display = '';
})

//음소거 버튼 이벤트핸들러
//muteBtn.addEventListener("click", handleMuteClick);
//카메라 버튼 이벤트핸들러
//cameraBtn.addEventListener("click", handleCameraClick);
//카메라선택 이벤트핸들러
// camerasSelect.addEventListener("input", handleCameraChange);

function handleMuteClick() {
  console.log('handleMuteClick...! myStream : %o', myStream);
  
  myStream
    .getAudioTracks()
    .forEach((track) => {
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

//카메라가 변경되면 변경된 카메라로 화면이 촬영되도록설정 
async function handleCameraChange() {
  try {
    await getMedia(camerasSelect.value);
    if (peerConnectionObjArr.length > 0) {
      const newVideoTrack = myStream.getVideoTracks()[0];
      peerConnectionObjArr.forEach((peerConnectionObj) => {
        const peerConnection = peerConnectionObj.connection;
        const peerVideoSender = peerConnection
          .getSenders()
          .find((sender) => sender.track.kind == "video");
        peerVideoSender.replaceTrack(newVideoTrack);
      });
    }
  } catch (error) {
    console.log(error);
  }
}


// Welcom Form (join a room)

const welcome = document.getElementById("welcome");
const welcomeForm = welcome.querySelector("form");

welcomeForm.addEventListener("submit", handlWelcomSubmit)

//회의들어가 버튼입력(submit)후 실행되는 함수
async function handlWelcomSubmit(event){
  event.preventDefault();

  enterRoom.style.display = 'none'; //방입장화면 미노출
  meetingMain.style.display = ''; //회의화면노출
  
  const inputRoom = welcomeForm.querySelector("input#roomName");
  const inputName = welcomeForm.querySelector("input#nickname");
  
  //media정보를 요청해, 방에 입장한 peer의 Stream정보를 가져와 비디오 및 카메라 정보를 그린다.  
  await initCall();

  //방에 입장했다는 이벤트를 emit하고, 'welcome'이벤트를 받을 준비를 하게 된다.
  socket.emit('join_room', inputRoom.value, inputName.value);
  roomName = inputRoom.value;
  nickname = inputName.value;

  //유저닉네임 셋팅
  const nicknameValue = document.querySelector("#userNickname");
  //nicknameValue.innerText = nickname;

  inputRoom.value = "";
  inputName.value = "";
}

//화상통화 init함수
async function initCall() { 
  await getMedia();

  //오디오생성 때문에 추가함
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
  
  //peer Connection에 오디오, 미디어 스트림을 넣는다
  myStream
    .getTracks()
    .forEach((track) => {
    console.log('makeConnection 호출 이후 track값 확인 : %o', track);
    myPeerConnection.addTrack(track, myStream);
  })

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
    myStream = await navigator.mediaDevices.getUserMedia(
      deviceId ? cameraConstraints : initialConstraints
    );

    //stream을 mute하는 것이 아니라 HTML video element를 mute한다.
    //이전버전
    //myFace.srcObject = myStream;
    //myFace.muted = true;

    const container = document.querySelector('.container');
    const div = document.createElement("div");
    div.classList.add('user');
    const video = document.createElement("video");
    video.autoplay = true;
    video.playsInline = true;
    video.srcObject = myStream; //비디오를 그린다.
    div.appendChild(video);
    container.appendChild(div);




    if (!deviceId) {
      // mute default
      myStream
        .getAudioTracks()
        .forEach((track) => (track.enabled = false));

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

      //카메라 선택하는 옵션DOm 코드
      //camerasSelect.appendChild(option);

    });
  } catch (error) {
    console.log(error);
  }
}

// Socket call
// 다른사람이 방에 들어올 경우 (예를들어 peer A가 받는 이벤트)
socket.on("welcome", async (userObjArr) => {
  console.log("somebody join...!");

  //같은방에 참가한 유저정보를 가져온다
  const length = userObjArr.length;
  if (length === 1) {
    return;
  }

  console.log(`회의에 입장한 유저수: ${length}`);
  //방금 입장한 유저를 제외한 이전유저들(Peer)
  for (let i = 0; i < length - 1; ++i) {
    try {
      console.log(userObjArr[i]);

      //피어연결을 위한 RTCPeerConnection객체를 생성
      //원격지 유저의 nickname을 인자로 전달해 RTC객체를 생성
      const newPeerConnection = makeConnection(
        userObjArr[i].socketId,
        userObjArr[i].nickname
      );
      
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

  //TODO: DataChannel - offer를 하기 전에, data channel을 만들어야 한다. 
  // myDataChannel = myPeerConnection.createDataChannel("chat");
  // myDataChannel.addEventListener("message", console.log);
  // console.log("made data channel");


})

//원격지에서 받는 offer 이벤트, answer를 생성해 전달한다.
socket.on("offer", async (offer, remoteSocketId, remoteNickname) => {
  //myPeerConnection.addEventListener("datachannel", console.log)
  console.log("received the offer");
  
  //remoteNickname: (ex: peerB의 닉네임)
  const newPeerConnection = makeConnection(remoteSocketId, remoteNickname);

  //전달된 offer를 setRemoteDescription 시킨다.
  newPeerConnection.setRemoteDescription(offer);
  console.log('received offer 이후 setRemoteDescription error!');

  const answer = await newPeerConnection.createAnswer();
  newPeerConnection.setLocalDescription(answer);

  console.log("sent the answer");
  socket.emit('answer', answer, remoteSocketId);
  
})

//peer A에서 전달받은 answer이벤트
//모든 브라우저가 remote, local description을 갖게된다
socket.on('answer', async (answer, remoteSocketId) => {
  console.log("received the answer : %o", answer, remoteSocketId);
  /**
   * RTCPeerConnection은 하나의 P2P연결만 수행하기 때문에, 
   * 3명이상의 참여자가 발생할 경우, 
   * 각 유저별 구분가능한 SocketId를 키값으로 RTCPeerConnection 각각 객체를 생성해
   * SDP응답을 처리한다. 
   */
  
  await pcObj[remoteSocketId].setRemoteDescription(answer);
})


// RTC Code

/** 
 * 각 피어 연결을 RTCPeerConnection객체에 의해 처리하는 함수.
 * stream정보를 내 peer에 넣는다.
 * 카메라가 바뀌면 deviceId가 바뀌기 때문에, stream도 같이 업데이트를 해준다.
 * 2명이상의 유저가 회의에 참가했을때, RTCPeerConnection객체를 새로 생성하기 위한 구분값으로 socketId를 전달한다.
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
  myStream
    .getTracks()
    .forEach((track) => {
    console.log('makeConnection 호출 이후 track값 확인 : %o', track);
    myPeerConnection.addTrack(track, myStream);
  })

  pcObj[remoteSocketId] = myPeerConnection;
  console.log(`socketId: ${remoteSocketId}`);
  console.log('pcObj[remoteSocketId] : %o', pcObj[remoteSocketId]);

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
  console.log("addIceCandidate...!, remoteSocketId : %o", remoteSocketId);
  await pcObj[remoteSocketId].addIceCandidate(ice);
})


//연결된 상대의 브라우저 스트림 정보를 받아, 상대방 비디오를 보여주게 된다.
function handleAddStream(data, remoteSocketId, remotenickname) {
  //console.log('addStream event fired..! : %o', data.stream);

  //@TODO: check this log to understand this code!!!! 
  // console.log("got an stream from my peer");
  // console.log("Peer's Stream", data.stream);
  // console.log("My Stream", myStream);  
  //peersFace.srcObject = data.stream;  
  //log확인용 소켓이벤트 발생 
  //socket.emit('addstream', data.stream);
  //paintPeerFace(data.stream, remoteSocketId, remotenickname);
  paintPeerFace2(data.stream, remoteSocketId, remotenickname);
}

//회의방에 입장한 다른유저Peer의 비디오stream정보를 받아 DOM에 그리는 함수
function paintPeerFace(peerStream, remoteSocketId, remotenickname) {
  const streams = document.querySelector(".screen__mobile");
  const div = document.createElement("div");
  div.id = remoteSocketId;
  const video = document.createElement("video");
  video.autoplay = true;
  video.playsInline = true;
  video.width = "400";
  video.height = "400";
  video.srcObject = peerStream; //상대방 비디오를 그린다.

  const nicknameContainer = document.createElement("h3");
  nicknameContainer.id = "usernickname";
  nicknameContainer.innerText = remotenickname;

  div.appendChild(video);
  div.appendChild(nicknameContainer);
  streams.appendChild(div);
  sortStreams();
}


//회의방에 입장한 다른유저Peer의 비디오stream정보를 받아 DOM에 그리는 함수
function paintPeerFace2(peerStream, remoteSocketId, remotenickname) {
  const container = document.querySelector('.container');
  const div = document.createElement("div");
  div.classList.add('user');
  const video = document.createElement("video");
  video.autoplay = true;
  video.playsInline = true;

  video.srcObject = peerStream; //상대방 비디오를 그린다.

  // const nicknameContainer = document.createElement("h3");
  // nicknameContainer.id = "usernickname";
  // nicknameContainer.innerText = remotenickname;

  div.appendChild(video);
  //div.appendChild(nicknameContainer);
  container.appendChild(div);
  //sortStreams();
}


//비디오스트림UI를 정렬하기 위한 클래스명추가 함수
function sortStreams() {
  const streams = document.querySelector(".screen__mobile");
  const streamArr = streams.querySelectorAll("div");
  streamArr.forEach((stream) => (stream.className = `people${peopleInRoom}`));
}
