'use strict'

import { API_SERVER_URL } from './config.js'


//1.회의코드검증
export function authMeeting(meetingnum, meetingpwd = '') {
  console.log('### 모듈에서 authMeetingNum() 함수 호출...!');

  const codeAuthUri = `${API_SERVER_URL}/api/meeting/verification?num=${meetingnum}&pwd=${meetingpwd}`;
  return fetch(codeAuthUri, {
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
    if (result.statusCode === 200) {
        console.log('#### 회의코드 또는 회의비밀번호 검증 성공...!');
        return result.data;
      }
  });
}


//회의검증에 성공하면, 응답 데이터를 받아 회의정보 팝업UI를 생성
export function createMeetingInfoPopup(data) {
  // console.log('createMeetingInfoPopup 호출!!!! data: %o', data);

  const popupUI = `<h3 class="modal__text">${data.title}</h3>
  <ul class="info__list">
    <li class="info__item">
      <span class="title">회의ID:  </span>
      ${data.meeting_num}
    </li>
    <li class="info__item">
      <span class="title">호스트ID:  </span>
      ${data.host_id}
    </li>
    <li class="info__item">
      <span class="title">암호:  </span>
      ${data.meeting_pwd}
    </li>
    <li class="info__item">
      <span class="title">최대 참여인원수:  </span>
      ${data.participants_num}명
    </li>
    <li class="info__item">
      <span class="title">회의기간:  </span>
      ${data.duration}
    </li>
  </ul>`;

  return popupUI;
}


//회의상태값 요청
export function getMeetingStatus(meetingnum) {
  const uri = `${API_SERVER_URL}/api/meeting/verification?num=${meetingnum}&pwd=`;
  return fetch(uri, {
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
    if (result.statusCode === 200) {
        console.log('#### 회의상태값 조회성공!!!');
        return result.data;
      }
  });
}


//회의상태값 업데이트
export function openMeetingStatus(hostId, meetingId) {
  const parameter = { hostId, meetingId };

  const uri = `${API_SERVER_URL}/api/meeting/status`;
  return fetch(uri, {
    method: 'POST',
    mode: 'cors', //해당 옵션을 주지 않으면, body객체가 서버로 전달되지 않는다.
    headers: { //JSON Type으로 보내기위한 헤더설정
      'Accept': 'application/json',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(parameter),
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
    if (result.statusCode === 200) {
        console.log(result.message);
        return true;
      }
  });
}


//회의ID로 회의정보 조회
export function getMeetingInfo(meetingId) {
  const uri = `${API_SERVER_URL}/api/meeting/info/${meetingId}`;
  return fetch(uri, {
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
    console.log('result : %o', result);
    if (result.statusCode === 200) {
      console.log('#### 회의 상세 조회성공!!!');
        return result.data;
      }
  });

}

//호스트가 회의를 나간 뒤 회의삭제 
export function deleteMeeting(meetinId) {
  const deleteUri = `${API_SERVER_URL}/api/meeting/info/${meetinId}`;
  return fetch(deleteUri, {
    method: 'DELETE',
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
    if (result.statusCode === 200) {
        console.log(result.message);
        return true;
      }
  });

}


//회의준비화면으로 이동하는 함수
export function moveToPrepareScreen(meetingNum, meetingpwd = '', nickname = '') {
  //페이지 이동시 파라미터를 넘기면서 이동
  location.replace(`${window.location.origin}/prepare?meetingnum=${meetingNum}&meetingpwd=${meetingpwd}&nickname=${nickname}`);
}

//회의상태정보 저장
export function saveScreenShare(mediatrack) {
  console.log('############### 화면공유 상태저장 함수 호출 전달값 확인...! : %o', mediatrack);

  const codeAuthUri = `${window.location.origin}/meeting/screen/share`;
  return fetch(codeAuthUri, {
    method: 'POST',
    mode: 'cors', //해당 옵션을 주지 않으면, body객체가 서버로 전달되지 않는다.
    headers: { //JSON Type으로 보내기위한 헤더설정
      'Accept': 'application/json',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(mediatrack.id),
  })
}