%define __jar_repack {%nil}
%define _prefix %{_usr}/lib/chronopolis
%define _confdir /etc/chronopolis
%define service ingest-server
%define build_time %(date +"%Y%m%d")

Name: ingest-server
Version: %{ver}
Release: %{build_time}%{?dist}
Source: ingest-server.jar
Source1: ingest-server.sh
Source2: application.properties
Summary: Chronopolis Ingest Server
License: UMD
URL: https://gitlab.umiacs.umd.edu/chronopolis
Group: System Environment/Daemons
Requires: /usr/sbin/groupadd, /usr/sbin/useradd, postgresql-server >= 8.1
autoprov: yes
autoreq: yes
BuildArch: noarch
BuildRoot: ${_tmppath}/build-%{name}-%{version}

%description
The Ingest Server hosts the API for handling bags, transfers, and
tokens.

%install

rm -rf "%{buildroot}"
%__install -D -m0644 "%{SOURCE0}" "%{buildroot}%{_prefix}/%{service}.jar"

%__install -d "%{buildroot}/var/log/chronopolis"
%__install -d "%{buildroot}/etc/chronopolis"

%__install -D -m0755 "%{SOURCE1}" "%{buildroot}/etc/init.d/%{service}"
%__install -D -m0600 "%{SOURCE2}" "%{buildroot}%{_confdir}/application.properties"


%files

%defattr(-,root,root)
# conf
%dir %{_confdir}
%config %attr(0644,-,-) %{_confdir}/application.properties
# jar
%dir %attr(0755,chronopolis,chronopolis) %{_prefix}
%{_prefix}/%{service}.jar
# init/log
%config(noreplace) /etc/init.d/%{service}
%dir %attr(0755,chronopolis,chronopolis) /var/log/chronopolis

%pre
/usr/sbin/groupadd -r chronopolis > /dev/null 2>&1 || :
/usr/sbin/useradd -r -g chronopolis -c "Chronopolis Service User" \
        -s /bin/bash -d /usr/lib/chronopolis/ chronopolis > /dev/null 2>&1 || :
